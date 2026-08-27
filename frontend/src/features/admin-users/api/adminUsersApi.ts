import type { UserRole } from '../../auth/types/auth'
import type { ManagedUser } from '../types/adminUser'

function accessToken() {
  const token = localStorage.getItem('devedu.accessToken') ?? localStorage.getItem('accessToken')
  if (!token) throw new Error('Bạn cần đăng nhập bằng tài khoản quản trị.')
  return token
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: { Authorization: `Bearer ${accessToken()}`, ...(init?.headers ?? {}) },
  })
  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as { message?: string } | null
    if (response.status === 401) throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.')
    if (response.status === 403) throw new Error('Chỉ ADMIN có thể quản lý người dùng.')
    throw new Error(body?.message ?? 'Không thể xử lý yêu cầu quản trị.')
  }
  return response.json() as Promise<T>
}

export const getManagedUsers = () => request<ManagedUser[]>('/api/admin/users')

export const updateManagedUserRole = (userId: string, role: UserRole) => request<ManagedUser>(
  `/api/admin/users/${encodeURIComponent(userId)}/role`,
  {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ role }),
  },
)
