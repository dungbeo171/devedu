import type { AuthenticationResponse, OAuthProvidersResponse } from '../types/auth'

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
