import type { AuthenticatedUser, AuthenticationResponse, OAuthProvidersResponse } from '../types/auth'

export async function authenticateWithEmail(
  mode: 'login' | 'register',
  email: string,
  password: string,
  name?: string,
): Promise<AuthenticationResponse> {
  const response = await fetch(`/api/auth/${mode}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(mode === 'register' ? { name, email, password } : { email, password }),
  })
  if (!response.ok) {
    const error = await response.json().catch(() => null) as { message?: string } | null
    throw new Error(error?.message ?? 'Không thể xác thực tài khoản.')
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
  if (!getStoredAccessToken()) return null
  try {
    const value = localStorage.getItem('devedu.user')
    if (!value) {
      clearAuthentication()
      return null
    }
    const user = JSON.parse(value) as AuthenticatedUser
    return { ...user, name: user.name?.trim() || user.email.split('@')[0] }
  } catch {
    clearAuthentication()
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
