import { getStoredAccessToken } from '../../auth/api/authApi'
import type { AdminAnalytics } from '../types/adminAnalytics'

export async function getAdminAnalytics(signal?: AbortSignal): Promise<AdminAnalytics> {
  const token = getStoredAccessToken()
  if (!token) throw new Error('Bạn cần đăng nhập bằng tài khoản quản trị.')
  const response = await fetch('/api/admin/analytics', {
    cache: 'no-store',
    signal,
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { message?: string } | null
    if (response.status === 401) throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.')
    if (response.status === 403) throw new Error('Chỉ ADMIN có thể xem thống kê hệ thống.')
    throw new Error(body?.message ?? 'Không thể tải thống kê hệ thống.')
  }
  return response.json() as Promise<AdminAnalytics>
}
