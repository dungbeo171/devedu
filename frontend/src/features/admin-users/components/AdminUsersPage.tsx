import { useEffect, useState } from 'react'
import type { UserRole } from '../../auth/types/auth'
import { getManagedUsers, updateManagedUserRole } from '../api/adminUsersApi'
import type { ManagedUser } from '../types/adminUser'
import { IconShield } from '../../../shared/components/Icons'

const roles: UserRole[] = ['STUDENT', 'TEACHER', 'ADMIN']
const roleLabels: Record<UserRole, string> = {
  STUDENT: 'Sinh viên',
  TEACHER: 'Giáo viên',
  ADMIN: 'Quản trị viên',
}

export function AdminUsersPage() {
  const currentUser = storedCurrentUser()
  const [users, setUsers] = useState<ManagedUser[]>([])
  const [loading, setLoading] = useState(true)
  const [updatingId, setUpdatingId] = useState<string | null>(null)
  const [message, setMessage] = useState('')

  useEffect(() => {
    if (currentUser?.role !== 'ADMIN') {
      setMessage('Chỉ tài khoản ADMIN có thể truy cập trang này.')
      setLoading(false)
      return
    }
    void getManagedUsers()
      .then(setUsers)
      .catch((error: unknown) =>
        setMessage(error instanceof Error ? error.message : 'Không thể tải người dùng.')
      )
      .finally(() => setLoading(false))
  }, [currentUser?.role])

  async function changeRole(user: ManagedUser, role: UserRole) {
    if (role === user.role || updatingId) return
    setUpdatingId(user.id)
    setMessage('')
    try {
      const updated = await updateManagedUserRole(user.id, role)
      setUsers((current) =>
        current.map((item) => (item.id === updated.id ? updated : item))
      )
      setMessage(`Đã cập nhật vai trò của ${updated.name} thành ${roleLabels[updated.role]}.`)
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Không thể cập nhật role.')
    } finally {
      setUpdatingId(null)
    }
  }

  return (
    <section>
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <div className="inline-flex items-center gap-2 rounded-xl border border-blue-500/30 bg-blue-500/10 px-3 py-1 font-mono text-[11px] font-bold uppercase tracking-wider text-blue-400">
            <IconShield className="h-3.5 w-3.5" />
            <span>Admin console</span>
          </div>
          <h2 className="mt-3 text-2xl font-black tracking-tight text-white sm:text-3xl">
            Quản lý người dùng
          </h2>
          <p className="mt-1 text-xs text-slate-400">
            Xem danh sách tài khoản hệ thống và cấp quyền Sinh viên, Giáo viên hoặc Quản trị viên.
          </p>
        </div>
        <div className="flex items-center gap-2 rounded-xl border border-white/10 bg-slate-900/80 px-3 py-1.5 text-xs font-semibold text-slate-300">
          <span className="h-2 w-2 rounded-full bg-blue-500" />
          <span>{loading ? 'Đang tải...' : `${users.length} tài khoản`}</span>
        </div>
      </div>

      {message ? (
        <div
          aria-live="polite"
          className="mt-6 rounded-2xl border border-blue-500/30 bg-blue-500/10 p-4 text-xs font-semibold text-blue-300"
        >
          {message}
        </div>
      ) : null}

      {!loading && currentUser?.role === 'ADMIN' ? (
        <div className="mt-6 overflow-x-auto rounded-3xl border border-white/10 bg-slate-900/80 shadow-2xl shadow-black/40 backdrop-blur-md">
          <table className="w-full min-w-[720px] text-left text-sm">
            <thead className="border-b border-white/10 bg-slate-950/60 font-mono text-[11px] font-bold uppercase tracking-wider text-slate-400">
              <tr>
                <th className="px-6 py-4">Người dùng</th>
                <th className="px-6 py-4">Email</th>
                <th className="px-6 py-4">Ngày đăng ký</th>
                <th className="px-6 py-4">Phân quyền (Role)</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => {
                const isCurrentAdmin =
                  user.id === currentUser.id || user.email === currentUser.email
                const isUpdating = updatingId === user.id
                return (
                  <tr
                    key={user.id}
                    className="border-t border-white/5 transition hover:bg-white/5"
                  >
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <span className="grid h-10 w-10 shrink-0 place-items-center rounded-2xl bg-gradient-to-br from-blue-600 to-indigo-600 font-mono text-sm font-bold text-white shadow-md shadow-blue-500/20 ring-1 ring-white/20">
                          {user.name.charAt(0).toLocaleUpperCase('vi')}
                        </span>
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="font-bold text-white">{user.name}</span>
                            {isCurrentAdmin ? (
                              <span className="rounded-md border border-blue-500/30 bg-blue-500/20 px-1.5 py-0.5 font-mono text-[10px] font-bold text-blue-300">
                                Bạn
                              </span>
                            ) : null}
                          </div>
                          <span className="font-mono text-[11px] text-slate-500">ID: {user.id.slice(0, 8)}...</span>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 font-mono text-xs text-slate-300">{user.email}</td>
                    <td className="px-6 py-4 text-xs font-medium text-slate-400">
                      {new Date(user.createdAt).toLocaleDateString('vi-VN', {
                        day: '2-digit',
                        month: '2-digit',
                        year: 'numeric',
                      })}
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        <select
                          aria-label={`Role của ${user.name}`}
                          value={user.role}
                          disabled={isCurrentAdmin || isUpdating}
                          onChange={(event) =>
                            void changeRole(user, event.target.value as UserRole)
                          }
                          className={`rounded-xl border px-3 py-1.5 font-mono text-xs font-bold outline-none transition disabled:cursor-not-allowed disabled:opacity-60 ${
                            user.role === 'ADMIN'
                              ? 'border-rose-500/30 bg-rose-500/10 text-rose-300 focus:border-rose-500'
                              : user.role === 'TEACHER'
                              ? 'border-indigo-500/30 bg-indigo-500/10 text-indigo-300 focus:border-indigo-500'
                              : 'border-blue-500/30 bg-blue-500/10 text-blue-300 focus:border-blue-500'
                          }`}
                        >
                          {roles.map((role) => (
                            <option key={role} value={role} className="bg-slate-900 text-white">
                              {roleLabels[role]}
                            </option>
                          ))}
                        </select>
                        {isUpdating ? (
                          <svg className="h-4 w-4 animate-spin text-blue-400" viewBox="0 0 24 24" fill="none">
                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                          </svg>
                        ) : null}
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      ) : null}
    </section>
  )
}

function storedCurrentUser(): ManagedUser | null {
  try {
    const value = localStorage.getItem('devedu.user')
    return value ? (JSON.parse(value) as ManagedUser) : null
  } catch {
    return null
  }
}
