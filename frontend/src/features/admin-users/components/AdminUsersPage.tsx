import { useEffect, useMemo, useState } from 'react'
import type { UserRole } from '../../auth/types/auth'
import { deleteManagedUser, getManagedUsers, updateManagedUser, updateManagedUserRole } from '../api/adminUsersApi'
import type { ManagedUser } from '../types/adminUser'
import { IconFilter, IconSearch, IconShield } from '../../../shared/components/Icons'
import { ModalDialog } from '../../../shared/components/ModalDialog'

const roles: UserRole[] = ['STUDENT', 'TEACHER', 'ADMIN']
const roleLabels: Record<UserRole, string> = {
  STUDENT: 'Sinh viên',
  TEACHER: 'Giáo viên',
  ADMIN: 'Quản trị viên',
}

type RoleFilter = 'ALL' | UserRole
type Notice = { message: string; tone: 'success' | 'error' }
type EditForm = { name: string; email: string; password: string; role: UserRole }

export function AdminUsersPage() {
  const currentUser = storedCurrentUser()
  const [users, setUsers] = useState<ManagedUser[]>([])
  const [loading, setLoading] = useState(true)
  const [updatingId, setUpdatingId] = useState<number | null>(null)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<ManagedUser | null>(null)
  const [editTarget, setEditTarget] = useState<ManagedUser | null>(null)
  const [editForm, setEditForm] = useState<EditForm | null>(null)
  const [savingEdit, setSavingEdit] = useState(false)
  const [editError, setEditError] = useState('')
  const [query, setQuery] = useState('')
  const [roleFilter, setRoleFilter] = useState<RoleFilter>('ALL')
  const [notice, setNotice] = useState<Notice>({ message: '', tone: 'success' })

  useEffect(() => {
    if (currentUser?.role !== 'ADMIN') {
      setNotice({ message: 'Chỉ tài khoản ADMIN có thể truy cập trang này.', tone: 'error' })
      setLoading(false)
      return
    }
    void getManagedUsers()
      .then(setUsers)
      .catch((error: unknown) => setNotice({ message: messageOf(error, 'Không thể tải người dùng.'), tone: 'error' }))
      .finally(() => setLoading(false))
  }, [currentUser?.role])

  useEffect(() => {
    if (!notice.message) return
    const timer = window.setTimeout(() => setNotice((current) => ({ ...current, message: '' })), 3200)
    return () => clearTimeout(timer)
  }, [notice.message])

  const visibleUsers = useMemo(() => {
    const keyword = query.trim().toLocaleLowerCase('vi')
    return users.filter((user) => {
      const matchesRole = roleFilter === 'ALL' || user.role === roleFilter
      const searchable = `${user.name} ${user.email} ${user.studentCode ?? ''} ${user.teacherCode ?? ''}`
      return matchesRole && (!keyword || searchable.toLocaleLowerCase('vi').includes(keyword))
    })
  }, [query, roleFilter, users])

  async function changeRole(user: ManagedUser, role: UserRole) {
    if (role === user.role || updatingId || deletingId) return
    setUpdatingId(user.id)
    try {
      const updated = await updateManagedUserRole(user.id, role)
      setUsers((current) => current.map((item) => (item.id === updated.id ? updated : item)))
      setNotice({ message: `Đã cập nhật vai trò của ${updated.name} thành ${roleLabels[updated.role]}.`, tone: 'success' })
    } catch (error) {
      setNotice({ message: messageOf(error, 'Không thể cập nhật role.'), tone: 'error' })
    } finally {
      setUpdatingId(null)
    }
  }

  function beginEdit(user: ManagedUser) {
    setEditTarget(user)
    setEditForm({ name: user.name, email: user.email, password: '', role: user.role })
    setEditError('')
  }

  async function saveUser() {
    if (!editTarget || !editForm || savingEdit) return
    setSavingEdit(true)
    setEditError('')
    try {
      const updated = await updateManagedUser(editTarget.id, {
        name: editForm.name,
        email: editForm.email,
        password: editForm.password || undefined,
        role: editForm.role,
      })
      setUsers((current) => current.map((user) => user.id === updated.id ? updated : user))
      setEditTarget(null); setEditForm(null)
      setNotice({ message: `Đã cập nhật tài khoản ${updated.name}.`, tone: 'success' })
    } catch (error) {
      setEditError(messageOf(error, 'Không thể cập nhật tài khoản.'))
    } finally {
      setSavingEdit(false)
    }
  }

  async function deleteUser() {
    if (!deleteTarget || deletingId) return
    setDeletingId(deleteTarget.id)
    try {
      await deleteManagedUser(deleteTarget.id)
      setUsers((current) => current.filter((user) => user.id !== deleteTarget.id))
      setNotice({ message: `Đã xóa tài khoản ${deleteTarget.name}.`, tone: 'success' })
      setDeleteTarget(null)
    } catch (error) {
      setNotice({ message: deletionMessage(error), tone: 'error' })
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <section>
      <div className="ui-page-header">
        <div>
          <div className="ui-kicker"><IconShield className="h-3.5 w-3.5" /><span>Quản trị hệ thống</span></div>
          <h1 className="ui-page-title mt-2">Quản lý người dùng</h1>
          <p className="ui-page-description">Tìm, sửa thông tin, cấp quyền và xóa tài khoản Giáo viên hoặc Sinh viên.</p>
        </div>
        <div className="ui-badge"><span className="h-2 w-2 rounded-full bg-blue-500" /><span>{loading ? 'Đang tải...' : `${users.length} tài khoản`}</span></div>
      </div>

      {!loading && currentUser?.role === 'ADMIN' ? (
        <>
          <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <label className="relative block w-full sm:max-w-md">
              <span className="sr-only">Tìm kiếm người dùng</span>
              <IconSearch className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input value={query} onChange={(event) => setQuery(event.target.value)} type="search" placeholder="Tìm tên, email hoặc mã giáo viên..." className="ui-control ui-control-with-leading-icon" />
            </label>
            <label className="flex shrink-0 items-center gap-2 text-xs font-semibold text-slate-600">
              <IconFilter className="h-4 w-4 text-slate-400" />
              <span>Vai trò</span>
              <select value={roleFilter} onChange={(event) => setRoleFilter(event.target.value as RoleFilter)} className="ui-control min-h-9 w-auto py-1.5 text-xs font-semibold">
                <option value="ALL">Tất cả</option>
                {roles.map((role) => <option key={role} value={role}>{roleLabels[role]}</option>)}
              </select>
            </label>
          </div>

          <div className="ui-panel mt-4 overflow-x-auto">
            <table className="w-full min-w-[820px] text-left text-sm">
              <thead className="border-b border-slate-200 bg-slate-50 text-xs font-bold text-slate-600">
                <tr><th className="px-6 py-4">Người dùng</th><th className="px-6 py-4">Email</th><th className="px-6 py-4">Ngày đăng ký</th><th className="px-6 py-4">Phân quyền</th><th className="px-6 py-4 text-right">Thao tác</th></tr>
              </thead>
              <tbody>
                {visibleUsers.map((user) => {
                  const isCurrentAdmin = user.id === currentUser.id || user.email === currentUser.email
                  const isUpdating = updatingId === user.id
                  const isDeleting = deletingId === user.id
                  return <tr key={user.id} className="border-t border-slate-100 transition hover:bg-blue-50/45">
                    <td className="px-6 py-4"><div className="flex items-center gap-3"><span className="grid h-10 w-10 shrink-0 place-items-center rounded-[10px] bg-blue-600 font-mono text-sm font-bold text-white shadow-sm">{user.name.charAt(0).toLocaleUpperCase('vi')}</span><div><div className="flex items-center gap-2"><span className="font-bold text-slate-950">{user.name}</span>{isCurrentAdmin ? <span className="rounded-md border border-blue-200 bg-blue-50 px-1.5 py-0.5 text-[10px] font-bold text-blue-700">Bạn</span> : null}</div><span className="font-mono text-[11px] text-slate-500">{user.role === 'ADMIN' ? 'Tài khoản quản trị' : `ID: ${user.publicId}${user.studentCode ? ` · ${user.studentCode}` : user.teacherCode ? ` · ${user.teacherCode}` : ''}`}</span></div></div></td>
                    <td className="px-6 py-4 font-mono text-xs text-slate-600">{user.email}</td>
                    <td className="px-6 py-4 text-xs font-medium text-slate-400">{new Date(user.createdAt).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })}</td>
                    <td className="px-6 py-4"><div className="flex items-center gap-2"><select aria-label={`Role của ${user.name}`} value={user.role} disabled={isCurrentAdmin || isUpdating || isDeleting} onChange={(event) => void changeRole(user, event.target.value as UserRole)} className={`rounded-xl border px-3 py-1.5 font-mono text-xs font-bold outline-none transition disabled:cursor-not-allowed disabled:opacity-60 ${user.role === 'ADMIN' ? 'border-red-200 bg-red-50 text-red-700 focus:border-red-500' : user.role === 'TEACHER' ? 'border-blue-200 bg-blue-50 text-blue-800 focus:border-blue-500' : 'border-slate-200 bg-white text-slate-700 focus:border-blue-500'}`}>
                        {roles.map((role) => <option key={role} value={role} className="bg-white text-slate-900">{roleLabels[role]}</option>)}
                      </select>{isUpdating || isDeleting ? <span className="h-4 w-4 animate-spin rounded-full border-2 border-blue-100 border-t-blue-600" aria-label="Đang xử lý" /> : null}</div></td>
                    <td className="px-6 py-4 text-right"><div className="flex justify-end gap-2">{user.role === 'TEACHER' || user.role === 'STUDENT' ? <><button type="button" disabled={isDeleting || isUpdating} onClick={() => beginEdit(user)} className="min-h-9 cursor-pointer rounded-lg border border-slate-200 bg-white px-3 text-xs font-bold text-slate-700 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700 disabled:cursor-not-allowed disabled:opacity-60">Sửa</button><button type="button" disabled={isDeleting || isUpdating} onClick={() => setDeleteTarget(user)} className="min-h-9 cursor-pointer rounded-lg border border-red-200 bg-white px-3 text-xs font-bold text-red-700 transition hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-60">Xóa</button></> : <span className="text-xs text-slate-400">—</span>}</div></td>
                  </tr>
                })}
              </tbody>
            </table>
            {visibleUsers.length === 0 ? <div className="py-12 text-center text-sm font-medium text-slate-500">Không tìm thấy tài khoản phù hợp.</div> : null}
          </div>
        </>
      ) : null}

      {loading ? <div className="ui-skeleton mt-6 h-72 rounded-[18px]" /> : null}
      {notice.message ? <div aria-live="polite" className={`fixed bottom-5 right-5 z-[90] max-w-[calc(100vw-2.5rem)] rounded-xl px-4 py-3 text-sm font-bold text-white shadow-xl transition ${notice.tone === 'error' ? 'bg-red-600' : 'bg-blue-700'}`}>{notice.message}</div> : null}
      {editTarget && editForm ? <EditUserModal target={editTarget} form={editForm} currentAdminId={currentUser?.id ?? 0} saving={savingEdit} error={editError} onChange={setEditForm} onClose={() => { if (!savingEdit) { setEditTarget(null); setEditForm(null) } }} onSave={() => void saveUser()} /> : null}
      {deleteTarget ? <ModalDialog title="Xóa tài khoản?" onClose={() => !deletingId && setDeleteTarget(null)}><div className="space-y-5 p-5 sm:p-6"><p className="text-sm leading-6 text-slate-600">Bạn có chắc chắn muốn xóa tài khoản <strong className="text-slate-900">{deleteTarget.name}</strong>? Thao tác này không thể hoàn tác.</p><div className="flex flex-col-reverse gap-3 border-t border-slate-200 pt-5 sm:flex-row sm:justify-end"><button type="button" disabled={Boolean(deletingId)} onClick={() => setDeleteTarget(null)} className="ui-button-secondary">Hủy</button><button type="button" disabled={Boolean(deletingId)} onClick={() => void deleteUser()} className="min-h-10 rounded-xl bg-red-600 px-4 text-sm font-bold text-white transition hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-60">{deletingId ? 'Đang xóa...' : 'Xóa tài khoản'}</button></div></div></ModalDialog> : null}
    </section>
  )
}

function EditUserModal({
  target,
  form,
  currentAdminId,
  saving,
  error,
  onChange,
  onClose,
  onSave,
}: {
  target: ManagedUser
  form: EditForm
  currentAdminId: number
  saving: boolean
  error: string
  onChange: (form: EditForm) => void
  onClose: () => void
  onSave: () => void
}) {
  const isCurrentAdmin = target.id === currentAdminId
  return <ModalDialog title="Sửa tài khoản" onClose={onClose}><form onSubmit={(event) => { event.preventDefault(); onSave() }} className="space-y-4 p-5 sm:p-6"><label className="block text-sm font-semibold text-slate-700">Họ và tên<input required value={form.name} onChange={(event) => onChange({ ...form, name: event.target.value })} className="ui-control mt-2 font-normal" /></label><label className="block text-sm font-semibold text-slate-700">Email<input required type="email" value={form.email} onChange={(event) => onChange({ ...form, email: event.target.value })} className="ui-control mt-2 font-normal" /></label><label className="block text-sm font-semibold text-slate-700">Mật khẩu mới <span className="font-normal text-slate-400">(để trống nếu không đổi)</span><input type="password" minLength={8} value={form.password} onChange={(event) => onChange({ ...form, password: event.target.value })} className="ui-control mt-2 font-normal" /></label><label className="block text-sm font-semibold text-slate-700">Vai trò<select value={form.role} disabled={isCurrentAdmin} onChange={(event) => onChange({ ...form, role: event.target.value as UserRole })} className="ui-control mt-2 font-normal disabled:cursor-not-allowed disabled:bg-slate-100">{roles.map((role) => <option key={role} value={role}>{roleLabels[role]}</option>)}</select></label>{error ? <p className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm font-semibold text-red-700">{error}</p> : null}<div className="flex flex-col-reverse gap-3 border-t border-slate-200 pt-5 sm:flex-row sm:justify-end"><button type="button" disabled={saving} onClick={onClose} className="ui-button-secondary">Hủy</button><button type="submit" disabled={saving} className="ui-button-primary">{saving ? 'Đang lưu...' : 'Lưu thay đổi'}</button></div></form></ModalDialog>
}

function messageOf(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

function deletionMessage(error: unknown) {
  const message = messageOf(error, 'Không thể xóa tài khoản.')
  return message.includes('related') ? 'Không thể xóa tài khoản này vì còn dữ liệu học tập hoặc giảng dạy liên quan. Hãy xử lý dữ liệu đó trước.' : message
}

function storedCurrentUser(): ManagedUser | null {
  try {
    const value = localStorage.getItem('devedu.user')
    return value ? (JSON.parse(value) as ManagedUser) : null
  } catch {
    return null
  }
}
