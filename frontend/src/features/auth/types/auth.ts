export type UserRole = 'STUDENT' | 'TEACHER' | 'ADMIN'
export type OAuthProvider = 'google' | 'github'

export interface AuthenticatedUser {
  id: string
  name: string
  email: string
  role: UserRole
  createdAt: string
}

export interface AuthenticationResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
  user: AuthenticatedUser
}

export interface OAuthProvidersResponse {
  enabledProviders: OAuthProvider[]
}
