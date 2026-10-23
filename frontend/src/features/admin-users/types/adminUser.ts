import type { AuthenticatedUser } from '../../auth/types/auth'

export type ManagedUser = AuthenticatedUser

export interface EmailVerificationSettings {
  enabled: boolean
  smtpConfigured: boolean
}
