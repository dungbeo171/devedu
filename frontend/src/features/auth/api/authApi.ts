import type {
  AuthenticatedUser,
  AuthenticationResponse,
  OAuthProvidersResponse,
  RegistrationVerificationResponse,
  UserRole,
} from '../types/auth'

async function readApiError(response: Response, fallback: string) {
  const error = await response.json().catch(() => null) as { message?: string } | null
  return error?.message ?? fallback
}

export async function loginWithEmail(
  email: string,
  password: string,
): Promise<AuthenticationResponse> {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
  if (!response.ok) {
    throw new Error(await readApiError(response, 'Không thể đăng nhập.'))
  }
  return response.json() as Promise<AuthenticationResponse>
}

export async function requestEmailRegistration(
  name: string,
  email: string,
  password: string,
): Promise<RegistrationVerificationResponse> {
  const response = await fetch('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, email, password }),
  })
  if (!response.ok) {
    throw new Error(await readApiError(response, 'Không thể gửi mã xác nhận.'))
  }
  return response.json() as Promise<RegistrationVerificationResponse>
}

export async function verifyEmailRegistration(
  email: string,
  code: string,
): Promise<AuthenticationResponse> {
  const response = await fetch('/api/auth/register/verify', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, code }),
  })
  if (!response.ok) {
    throw new Error(await readApiError(response, 'Không thể xác nhận email.'))
  }
  return response.json() as Promise<AuthenticationResponse>
}

export async function getOAuthProviders(): Promise<OAuthProvidersResponse> {
  const response = await fetch('/api/auth/oauth/providers')
  if (!response.ok) throw new Error('Không thể tải cấu hình đăng nhập mạng xã hội.')
  return response.json() as Promise<OAuthProvidersResponse>
}

export function saveAuthentication(result: AuthenticationResponse) {
  localStorage.setItem('devedu.accessToken', result.accessToken)
  localStorage.setItem('devedu.user', JSON.stringify(result.user))
}

export function clearAuthentication() {
  localStorage.removeItem('devedu.accessToken')
  localStorage.removeItem('accessToken')
  localStorage.removeItem('devedu.user')
}

export function getStoredAccessToken(): string | null {
  const token = localStorage.getItem('devedu.accessToken') ?? localStorage.getItem('accessToken')
  if (!token || isExpiredJwt(token)) {
    clearAuthentication()
    return null
  }
  return token
}

export function getStoredUser(): AuthenticatedUser | null {
  const token = getStoredAccessToken()
  if (!token) return null
  try {
    const value = localStorage.getItem('devedu.user')
    if (!value) {
      clearAuthentication()
      return null
    }
    const user = JSON.parse(value) as AuthenticatedUser
    const tokenRole = roleFromJwt(token)
    const synchronized = { ...user, role: tokenRole ?? user.role,
      name: user.name?.trim() || user.email.split('@')[0] }
    if (tokenRole && tokenRole !== user.role) localStorage.setItem('devedu.user', JSON.stringify(synchronized))
    return synchronized
  } catch {
    clearAuthentication()
    return null
  }
}

function roleFromJwt(token: string): UserRole | null {
  try {
    const encodedPayload = token.split('.')[1]
    if (!encodedPayload) return null
    const normalized = encodedPayload.replace(/-/g, '+').replace(/_/g, '/')
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=')
    const role = (JSON.parse(atob(padded)) as { role?: unknown }).role
    return role === 'STUDENT' || role === 'TEACHER' || role === 'ADMIN' ? role : null
  } catch {
    return null
  }
}

function isExpiredJwt(token: string): boolean {
  try {
    const encodedPayload = token.split('.')[1]
    if (!encodedPayload) return true
    const normalized = encodedPayload.replace(/-/g, '+').replace(/_/g, '/')
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=')
    const payload = JSON.parse(atob(padded)) as { exp?: number }
    return typeof payload.exp !== 'number' || payload.exp <= Math.floor(Date.now() / 1000) + 5
  } catch {
    return true
  }
}
