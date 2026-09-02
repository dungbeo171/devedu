import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { addCourseStudents, removeCourseStudents, searchCourseStudentCandidates, updateCourseStudent } from '../api/courseLearningApi'
import type { CourseStudent, CourseStudentCandidate, ManagedCourse } from '../types/courseLearning'
import { TeacherCourseProblems } from './TeacherCourseProblems'

type ClassTab = 'STUDENTS' | 'PROBLEMS'
type Removal = { ids: number[]; studentName?: string }
const PAGE_SIZE = 10

export function ClassStudentManagement({ course, students, loading, error, onBack, onRoster, onToast, onRetry }: {
  course: ManagedCourse
  students: CourseStudent[]
  loading: boolean
  error: string
  onBack: () => void
  onRoster: (students: CourseStudent[]) => void
  onToast: (message: string) => void
  onRetry: () => void
}) {
  const [tab, setTab] = useState<ClassTab>('STUDENTS')
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState<'ALL' | 'ACTIVE'>('ALL')
  const [selected, setSelected] = useState<Set<number>>(new Set())
  const [page, setPage] = useState(1)
  const [addOpen, setAddOpen] = useState(false)
  const [removal, setRemoval] = useState<Removal | null>(null)
  const [removing, setRemoving] = useState(false)
  const [editing, setEditing] = useState<CourseStudent | null>(null)
  const [editName, setEditName] = useState('')
  const [editBusy, setEditBusy] = useState(false)

  const filtered = useMemo(() => {
    const keyword = query.trim().toLocaleLowerCase('vi')
    return students.filter((student) => (status === 'ALL' || student.status === status)
      && (!keyword || `${student.name} ${student.email} ${student.studentCode}`.toLocaleLowerCase('vi').includes(keyword)))
  }, [students, query, status])
  const pageCount = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const pageItems = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE)
  const allOnPage = pageItems.length > 0 && pageItems.every((student) => selected.has(student.id))

  useEffect(() => { setPage(1) }, [query, status])
  useEffect(() => { if (page > pageCount) setPage(pageCount) }, [page, pageCount])

  async function confirmRemoval() {
    if (!removal) return
    setRemoving(true)
    try {
      onRoster(await removeCourseStudents(course.id, removal.ids))
      setSelected(new Set())
      setRemoval(null)
      onToast(removal.ids.length === 1 ? 'Đã xóa sinh viên khỏi lớp' : 'Đã xóa các sinh viên đã chọn')
    } catch (reason) {
      onToast(errorMessage(reason, 'Không thể xóa sinh viên.'))
    } finally {
      setRemoving(false)
    }
  }
  async function saveStudentName() {
    if (!editing || !editName.trim()) return
    setEditBusy(true)
    try { onRoster(await updateCourseStudent(course.id, editing.id, editName.trim())); setEditing(null); onToast('Đã cập nhật sinh viên') }
    catch (reason) { onToast(errorMessage(reason, 'Không thể cập nhật sinh viên.')) }
    finally { setEditBusy(false) }
  }

  const tabs: [ClassTab, string][] = [['STUDENTS', 'Sinh viên'], ['PROBLEMS', 'Bài tập']]

  return <>
    <button type="button" onClick={onBack} className="mb-4 cursor-pointer text-sm font-bold text-blue-700 hover:text-blue-900">← Danh sách lớp học</button>
    <header className="rounded-3xl border border-blue-100 bg-gradient-to-br from-blue-50 via-white to-white p-5 shadow-sm sm:p-8">
      <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <p className="font-mono text-xs font-extrabold text-blue-700">{course.code}</p>
          <h1 id="classroom-title" className="mt-2 text-2xl font-black text-slate-950 sm:text-3xl">{course.title}</h1>
          <div className="mt-4 flex flex-wrap items-center gap-x-6 gap-y-2 text-sm text-slate-600">
            <span>Mã lớp: <b className="text-slate-900">{course.code}</b></span>
            <span>Giảng viên: <b className="text-slate-900">{course.teacherName}</b></span>
            <span>Số sinh viên: <b className="text-slate-900">{course.studentCount}</b></span>
            <span className={`rounded-full px-2.5 py-1 text-xs font-bold ${course.status === 'ACTIVE' ? 'bg-blue-100 text-blue-700' : 'bg-slate-100 text-slate-600'}`}>{course.status === 'ACTIVE' ? 'Đang hoạt động' : 'Đã kết thúc'}</span>
          </div>
        </div>
        <button type="button" onClick={() => setAddOpen(true)} className="min-h-11 w-full cursor-pointer rounded-xl bg-blue-600 px-5 text-sm font-bold text-white shadow-md shadow-blue-200 hover:bg-blue-700 sm:w-auto">+ Thêm sinh viên</button>
      </div>
    </header>

    <nav className="mt-6 flex gap-1 overflow-x-auto border-b border-slate-200" aria-label="Nội dung lớp học">
      {tabs.map(([value, label]) => <button key={value} type="button" onClick={() => setTab(value)} className={`shrink-0 cursor-pointer border-b-2 px-4 py-3 text-sm font-bold ${tab === value ? 'border-blue-600 text-blue-700' : 'border-transparent text-slate-500 hover:text-blue-700'}`}>{label}</button>)}
    </nav>

    {tab === 'PROBLEMS' ? <TeacherCourseProblems course={course} onToast={onToast} /> : <section className="mt-6" aria-label="Danh sách sinh viên">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
        <Search value={query} onChange={setQuery} />
        <select value={status} onChange={(event) => setStatus(event.target.value as 'ALL' | 'ACTIVE')} className="h-11 w-full cursor-pointer rounded-xl border border-slate-200 bg-white px-4 text-sm font-semibold outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-50 sm:w-auto">
          <option value="ALL">Tất cả trạng thái</option><option value="ACTIVE">Đang học</option>
        </select>
        {selected.size > 0 ? <button type="button" onClick={() => setRemoval({ ids: [...selected] })} className="h-11 w-full cursor-pointer rounded-xl border border-red-200 px-4 text-sm font-bold text-red-600 hover:bg-red-50 sm:w-auto">Xóa {selected.size} sinh viên</button> : null}
      </div>

      {loading ? <RosterSkeleton /> : error ? <ErrorState message={error} onRetry={onRetry} /> : students.length === 0 ? <EmptyRoster onAdd={() => setAddOpen(true)} /> : <>
        <div className="mt-4 hidden overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm md:block">
          <div className="overflow-x-auto"><table className="w-full min-w-[680px] text-left text-sm">
            <thead className="bg-blue-50 text-xs uppercase text-blue-800"><tr>
              <th className="px-4 py-3"><input aria-label="Chọn tất cả sinh viên trang này" type="checkbox" checked={allOnPage} onChange={() => setSelected(allOnPage ? new Set([...selected].filter((id) => !pageItems.some((student) => student.id === id))) : new Set([...selected, ...pageItems.map((student) => student.id)]))} className="h-4 w-4 cursor-pointer accent-blue-600" /></th>
              <th className="px-4 py-3">Sinh viên</th><th className="hidden px-4 py-3 lg:table-cell">Email</th><th className="px-4 py-3">Mã sinh viên</th><th className="hidden px-4 py-3 xl:table-cell">Ngày tham gia</th><th className="px-4 py-3">Trạng thái</th><th className="px-4 py-3 text-right">Thao tác</th>
            </tr></thead>
            <tbody className="divide-y divide-slate-100">{pageItems.map((student) => <tr key={student.id} className="hover:bg-blue-50/40">
              <td className="px-4 py-3"><StudentCheckbox student={student} selected={selected} setSelected={setSelected} /></td>
              <td className="px-4 py-3"><div className="flex items-center gap-3"><Avatar name={student.name} /><b>{student.name}</b></div></td>
              <td className="hidden px-4 py-3 text-slate-600 lg:table-cell">{student.email}</td><td className="px-4 py-3 font-mono font-bold text-blue-700">{student.studentCode}</td><td className="hidden px-4 py-3 text-slate-600 xl:table-cell">{formatDate(student.joinedAt)}</td><td className="px-4 py-3"><ActiveBadge /></td>
              <td className="px-4 py-3"><div className="flex justify-end gap-1"><button type="button" onClick={() => { setEditing(student); setEditName(student.name) }} className="cursor-pointer rounded-lg px-3 py-2 font-bold text-blue-700 hover:bg-blue-50">Sửa</button><button type="button" onClick={() => setRemoval({ ids: [student.id], studentName: student.name })} className="cursor-pointer rounded-lg px-3 py-2 font-bold text-red-600 hover:bg-red-50">Xóa khỏi lớp</button></div></td>
            </tr>)}</tbody>
          </table></div>
        </div>

        <div className="mt-4 grid gap-3 md:hidden">{pageItems.map((student) => <article key={student.id} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
          <div className="flex items-start gap-3"><StudentCheckbox student={student} selected={selected} setSelected={setSelected} /><Avatar name={student.name} /><div className="min-w-0 flex-1"><b className="block truncate text-slate-900">{student.name}</b><p className="truncate text-xs text-slate-500">{student.email}</p><p className="mt-1 font-mono text-xs font-bold text-blue-700">{student.studentCode}</p></div><ActiveBadge /></div>
          <div className="mt-3 flex flex-col gap-2 border-t border-slate-100 pt-3 sm:flex-row sm:items-center sm:justify-between"><span className="text-xs text-slate-500">Tham gia {formatDate(student.joinedAt)}</span><div className="flex justify-end gap-1"><button type="button" onClick={() => { setEditing(student); setEditName(student.name) }} className="cursor-pointer rounded-lg px-3 py-2 text-xs font-bold text-blue-700 hover:bg-blue-50">Sửa</button><button type="button" onClick={() => setRemoval({ ids: [student.id], studentName: student.name })} className="cursor-pointer rounded-lg px-3 py-2 text-xs font-bold text-red-600 hover:bg-red-50">Xóa khỏi lớp</button></div></div>
        </article>)}</div>
        {filtered.length === 0 ? <p className="mt-4 rounded-2xl border border-slate-200 bg-white p-8 text-center text-sm text-slate-500">Không tìm thấy sinh viên phù hợp.</p> : <Pagination page={page} pages={pageCount} total={students.length} filteredTotal={filtered.length} onPage={setPage} />}
      </>}
    </section>}

    {addOpen ? <AddStudentsModal course={course} onClose={() => setAddOpen(false)} onNotice={onToast} onAdded={(roster) => { onRoster(roster); setAddOpen(false); onToast('Đã thêm sinh viên vào lớp') }} /> : null}
    {editing ? <Modal title="Sửa sinh viên trong lớp" onClose={() => !editBusy && setEditing(null)}><div className="p-5 sm:p-6"><label className="block text-sm font-bold text-slate-700">Tên hiển thị trong lớp<input value={editName} maxLength={100} onChange={(event) => setEditName(event.target.value)} className="mt-2 h-11 w-full rounded-xl border border-slate-200 px-3.5 font-normal outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-50" /></label><p className="mt-2 text-xs text-slate-500">Tên này chỉ được sử dụng trong lớp, không thay đổi tài khoản của sinh viên.</p><div className="mt-5 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end"><button type="button" onClick={() => setEditing(null)} disabled={editBusy} className="min-h-11 cursor-pointer rounded-xl border border-slate-200 px-5 text-sm font-bold">Hủy</button><button type="button" onClick={() => void saveStudentName()} disabled={editBusy || !editName.trim()} className="min-h-11 cursor-pointer rounded-xl bg-blue-600 px-5 text-sm font-bold text-white disabled:opacity-50">{editBusy ? 'Đang lưu...' : 'Lưu thay đổi'}</button></div></div></Modal> : null}
    {removal ? <ConfirmationDialog title="Xóa sinh viên khỏi lớp?" message={removal.studentName ? `Bạn có chắc chắn muốn xóa ${removal.studentName} khỏi lớp ${course.title}?` : `Bạn có chắc chắn muốn xóa ${removal.ids.length} sinh viên đã chọn khỏi lớp ${course.title}?`} busy={removing} onCancel={() => !removing && setRemoval(null)} onConfirm={() => void confirmRemoval()} /> : null}
  </>
}

function AddStudentsModal({ course, onClose, onAdded, onNotice }: { course: ManagedCourse; onClose: () => void; onAdded: (students: CourseStudent[]) => void; onNotice: (message: string) => void }) {
  const [tab, setTab] = useState<'SEARCH' | 'INVITE'>('SEARCH')
  const [query, setQuery] = useState('')
  const [students, setStudents] = useState<CourseStudentCandidate[]>([])
  const [selected, setSelected] = useState<Set<number>>(new Set())
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  useEffect(() => {
    if (tab !== 'SEARCH') return
    let active = true
    const timer = window.setTimeout(async () => {
      setLoading(true); setError('')
      try { const result = await searchCourseStudentCandidates(course.id, query); if (active) setStudents(result) }
      catch (reason) { if (active) setError(errorMessage(reason, 'Không thể tìm sinh viên.')) }
      finally { if (active) setLoading(false) }
    }, 250)
    return () => { active = false; clearTimeout(timer) }
  }, [course.id, query, tab])
  async function add() { if (!selected.size) return; setSaving(true); setError(''); try { onAdded(await addCourseStudents(course.id, [...selected])) } catch (reason) { setError(errorMessage(reason, 'Không thể thêm sinh viên.')); setSaving(false) } }
  async function copyCode() { try { await navigator.clipboard.writeText(course.code); onNotice('Đã sao chép mã lớp') } catch { onNotice('Không thể sao chép mã lớp') } }

  return <Modal title="Thêm sinh viên vào lớp" onClose={() => !saving && onClose()}>
    <div className="flex border-b border-slate-100 px-5 pt-2 sm:px-6"><ModalTab active={tab === 'SEARCH'} onClick={() => setTab('SEARCH')}>Tìm kiếm sinh viên</ModalTab><ModalTab active={tab === 'INVITE'} onClick={() => setTab('INVITE')}>Mời bằng mã lớp</ModalTab></div>
    {tab === 'INVITE' ? <div className="p-5 sm:p-6"><div className="rounded-2xl border border-blue-100 bg-blue-50 p-5 text-center"><p className="text-xs font-extrabold uppercase tracking-wider text-blue-600">Mã lớp</p><code className="mt-3 block break-all text-2xl font-black text-blue-800">{course.code}</code><button type="button" onClick={() => void copyCode()} className="mt-5 min-h-11 w-full cursor-pointer rounded-xl bg-blue-600 px-5 text-sm font-bold text-white hover:bg-blue-700 sm:w-auto">Sao chép mã</button></div><p className="mt-4 text-center text-sm leading-6 text-slate-600">Chia sẻ mã lớp này cho sinh viên để họ có thể tham gia lớp học.</p><div className="mt-5 flex justify-end"><button type="button" onClick={onClose} className="min-h-11 w-full cursor-pointer rounded-xl border border-slate-200 px-5 text-sm font-bold text-slate-700 hover:bg-slate-50 sm:w-auto">Đóng</button></div></div> : <div className="p-5 sm:p-6">
      <Search value={query} onChange={setQuery} longPlaceholder />
      <div className="mt-4 max-h-80 overflow-y-auto rounded-xl border border-slate-200">{loading ? <div className="space-y-3 p-4">{[1, 2, 3].map((item) => <div key={item} className="h-14 animate-pulse rounded-xl bg-blue-50" />)}</div> : error ? <p className="p-4 text-sm font-semibold text-red-600">{error}</p> : students.length === 0 ? <p className="p-8 text-center text-sm text-slate-500">Không tìm thấy sinh viên chưa thuộc lớp.</p> : students.map((student) => <label key={student.id} className="flex cursor-pointer items-center gap-3 border-b border-slate-100 p-4 last:border-0 hover:bg-blue-50/50"><input type="checkbox" checked={selected.has(student.id)} onChange={() => toggleStudent(student.id, selected, setSelected)} className="h-4 w-4 accent-blue-600" /><Avatar name={student.name} /><span className="min-w-0 flex-1"><b className="block truncate text-sm">{student.name}</b><span className="block truncate text-xs text-slate-500">{student.email}</span></span><code className="text-xs font-bold text-blue-700">{student.studentCode}</code></label>)}</div>
      <div className="mt-5 flex flex-col gap-4 border-t border-slate-100 pt-5 sm:flex-row sm:items-center sm:justify-between"><b className="text-sm text-slate-700">Đã chọn: {selected.size} sinh viên</b><div className="flex flex-col-reverse gap-3 sm:flex-row"><button type="button" onClick={onClose} disabled={saving} className="min-h-11 cursor-pointer rounded-xl border border-slate-200 px-5 text-sm font-bold hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50">Hủy</button><button type="button" onClick={() => void add()} disabled={!selected.size || saving} className="min-h-11 cursor-pointer rounded-xl bg-blue-600 px-5 text-sm font-bold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50">{saving ? 'Đang thêm...' : 'Thêm sinh viên'}</button></div></div>
    </div>}
  </Modal>
}

function ConfirmationDialog({ title, message, busy, onCancel, onConfirm }: { title: string; message: string; busy: boolean; onCancel: () => void; onConfirm: () => void }) { return <Modal title={title} onClose={onCancel}><div className="p-5 sm:p-6"><p className="leading-6 text-slate-600">{message}</p><div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end"><button type="button" onClick={onCancel} disabled={busy} className="min-h-11 cursor-pointer rounded-xl border border-slate-200 px-5 text-sm font-bold hover:bg-slate-50 disabled:opacity-50">Hủy</button><button type="button" onClick={onConfirm} disabled={busy} className="min-h-11 cursor-pointer rounded-xl bg-red-600 px-5 text-sm font-bold text-white hover:bg-red-700 disabled:cursor-wait disabled:opacity-60">{busy ? 'Đang xóa...' : 'Xóa sinh viên'}</button></div></div></Modal> }
function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) { return <div className="fixed inset-0 z-[80] flex items-end justify-center bg-slate-950/45 p-0 backdrop-blur-[2px] sm:items-center sm:p-4" role="dialog" aria-modal="true" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><div className="max-h-[95vh] w-full overflow-y-auto rounded-t-3xl bg-white shadow-2xl sm:max-w-xl sm:rounded-3xl"><div className="flex items-center justify-between border-b border-slate-100 px-5 py-4 sm:px-6"><div><p className="text-xs font-extrabold uppercase tracking-[.16em] text-blue-600">DevEdu</p><h2 className="mt-1 text-xl font-black">{title}</h2></div><button type="button" onClick={onClose} aria-label="Đóng" className="grid h-9 w-9 cursor-pointer place-items-center rounded-full text-xl hover:bg-blue-50">×</button></div>{children}</div></div> }
function ModalTab({ active, onClick, children }: { active: boolean; onClick: () => void; children: ReactNode }) { return <button type="button" onClick={onClick} className={`cursor-pointer border-b-2 px-3 py-3 text-sm font-bold ${active ? 'border-blue-600 text-blue-700' : 'border-transparent text-slate-500 hover:text-blue-700'}`}>{children}</button> }
function Search({ value, onChange, longPlaceholder = false }: { value: string; onChange: (value: string) => void; longPlaceholder?: boolean }) { return <label className="relative block w-full flex-1 lg:max-w-md"><span className="sr-only">Tìm kiếm sinh viên</span><span className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400">⌕</span><input type="search" value={value} onChange={(event) => onChange(event.target.value)} placeholder={longPlaceholder ? 'Tìm kiếm theo tên, email hoặc mã sinh viên...' : 'Tìm kiếm sinh viên...'} className="h-11 w-full rounded-xl border border-slate-200 bg-slate-50 pl-10 pr-4 text-sm outline-none focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-50" /></label> }
function StudentCheckbox({ student, selected, setSelected }: { student: CourseStudent; selected: Set<number>; setSelected: React.Dispatch<React.SetStateAction<Set<number>>> }) { return <input aria-label={`Chọn ${student.name}`} type="checkbox" checked={selected.has(student.id)} onChange={() => toggleStudent(student.id, selected, setSelected)} className="h-4 w-4 cursor-pointer accent-blue-600" /> }
function toggleStudent(id: number, selected: Set<number>, setSelected: React.Dispatch<React.SetStateAction<Set<number>>>) { const next = new Set(selected); next.has(id) ? next.delete(id) : next.add(id); setSelected(next) }
function Pagination({ page, pages, total, filteredTotal, onPage }: { page: number; pages: number; total: number; filteredTotal: number; onPage: (page: number) => void }) { return <div className="mt-4 flex flex-col gap-3 rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm sm:flex-row sm:items-center sm:justify-between"><span className="text-center text-slate-600 sm:text-left">Tổng số: <b className="text-slate-900">{total} sinh viên</b>{filteredTotal !== total ? ` · ${filteredTotal} kết quả` : ''}</span><div className="flex items-center justify-center gap-2"><button type="button" disabled={page === 1} onClick={() => onPage(page - 1)} className="min-h-9 cursor-pointer rounded-lg border border-slate-200 px-3 font-bold text-slate-700 hover:bg-blue-50 disabled:cursor-not-allowed disabled:opacity-40">Previous</button><span className="min-w-20 text-center font-bold text-blue-700">{page} / {pages}</span><button type="button" disabled={page === pages} onClick={() => onPage(page + 1)} className="min-h-9 cursor-pointer rounded-lg border border-slate-200 px-3 font-bold text-slate-700 hover:bg-blue-50 disabled:cursor-not-allowed disabled:opacity-40">Next</button></div></div> }
function EmptyRoster({ onAdd }: { onAdd: () => void }) { return <div className="mt-4 rounded-3xl border border-dashed border-blue-200 bg-blue-50/40 px-5 py-12 text-center"><div className="mx-auto grid h-12 w-12 place-items-center rounded-2xl bg-white text-xl text-blue-600 shadow-sm">♟</div><h2 className="mt-4 text-lg font-black">Chưa có sinh viên</h2><p className="mt-2 text-sm text-slate-500">Hãy thêm sinh viên vào lớp để bắt đầu quản lý.</p><button type="button" onClick={onAdd} className="mt-5 min-h-11 cursor-pointer rounded-xl bg-blue-600 px-5 text-sm font-bold text-white hover:bg-blue-700">+ Thêm sinh viên</button></div> }
function ErrorState({ message, onRetry }: { message: string; onRetry: () => void }) { return <div className="mt-4 rounded-2xl border border-red-200 bg-red-50 p-6 text-center"><p className="text-sm font-semibold text-red-700">{message}</p><button type="button" onClick={onRetry} className="mt-4 min-h-10 cursor-pointer rounded-xl border border-red-200 bg-white px-4 text-sm font-bold text-red-700 hover:bg-red-100">Thử lại</button></div> }
function RosterSkeleton() { return <div className="mt-4 space-y-2 rounded-2xl border border-blue-100 bg-white p-4">{[1, 2, 3, 4, 5].map((item) => <div key={item} className="h-14 animate-pulse rounded-xl bg-blue-50" />)}</div> }
function Avatar({ name }: { name: string }) { return <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-blue-600 text-xs font-black text-white">{name.split(/\s+/).slice(-2).map((part) => part[0]).join('').toUpperCase()}</span> }
function ActiveBadge() { return <span className="whitespace-nowrap rounded-full bg-blue-50 px-2.5 py-1 text-xs font-bold text-blue-700">Đang học</span> }
function formatDate(value: string) { return !value || value.startsWith('1970-01-01') ? '—' : new Intl.DateTimeFormat('vi-VN').format(new Date(value)) }
function errorMessage(reason: unknown, fallback: string) { return reason instanceof globalThis.Error ? reason.message || fallback : fallback }
