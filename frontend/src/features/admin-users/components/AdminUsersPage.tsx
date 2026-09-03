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
  const [updatingId, setUpdatingId] = useState<number | null>(null)
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
      <div className="ui-page-header">
        <div>
          <div className="ui-kicker">
            <IconShield className="h-3.5 w-3.5" />
            <span>Quản trị hệ thống</span>
          </div>
          <h1 className="ui-page-title mt-2">
            Quản lý người dùng
          </h1>
          <p className="ui-page-description">
            Xem danh sách tài khoản hệ thống và cấp quyền Sinh viên, Giáo viên hoặc Quản trị viên.
          </p>
        </div>
        <div className="ui-badge">
          <span className="h-2 w-2 rounded-full bg-blue-500" />
          <span>{loading ? 'Đang tải...' : `${users.length} tài khoản`}</span>
        </div>
      </div>

      {message ? (
        <div
          aria-live="polite"
          className="mt-6 rounded-xl border border-blue-200 bg-blue-50 p-4 text-sm font-semibold text-blue-800"
        >
          {message}
        </div>
      ) : null}

      {loading ? <div className="ui-skeleton mt-6 h-72 rounded-[18px]" /> : null}

      {!loading && currentUser?.role === 'ADMIN' ? (
        <div className="ui-panel mt-6 overflow-x-auto">
          <table className="w-full min-w-[720px] text-left text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-xs font-bold text-slate-600">
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
                    className="border-t border-slate-100 transition hover:bg-blue-50/45"
                  >
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <span className="grid h-10 w-10 shrink-0 place-items-center rounded-[10px] bg-blue-600 font-mono text-sm font-bold text-white shadow-sm">
                          {user.name.charAt(0).toLocaleUpperCase('vi')}
                        </span>
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="font-bold text-slate-950">{user.name}</span>
                            {isCurrentAdmin ? (
                              <span className="rounded-md border border-blue-200 bg-blue-50 px-1.5 py-0.5 text-[10px] font-bold text-blue-700">
                                Bạn
                              </span>
                            ) : null}
                          </div>
                          <span className="font-mono text-[11px] text-slate-500">
                            {user.role === 'ADMIN'
                              ? 'Tài khoản quản trị'
                              : `ID: ${user.publicId}${user.studentCode ? ` · ${user.studentCode}` : user.teacherCode ? ` · ${user.teacherCode}` : ''}`}
                          </span>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 font-mono text-xs text-slate-600">{user.email}</td>
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
                              ? 'border-red-200 bg-red-50 text-red-700 focus:border-red-500'
                              : user.role === 'TEACHER'
                              ? 'border-blue-200 bg-blue-50 text-blue-800 focus:border-blue-500'
                              : 'border-slate-200 bg-white text-slate-700 focus:border-blue-500'
                          }`}
                        >
                          {roles.map((role) => (
                            <option key={role} value={role} className="bg-white text-slate-900">
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
