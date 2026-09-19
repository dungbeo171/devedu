import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { createCourse, deleteManagedCourse, getCourseStudents, getManagedCourses, updateManagedCourse } from '../api/courseLearningApi'
import type { CourseStatus, CourseStudent, ManagedCourse } from '../types/courseLearning'
import { getStoredUser } from '../../auth/api/authApi'
import { IconArrowRight, IconBookOpen, IconCalendar, IconCheckCircle, IconHelpCircle, IconUser } from '../../../shared/components/Icons'
import { ClassStudentManagement } from './ClassStudentManagement'
import { StudentClassrooms } from './StudentClassrooms'
import { ModalDialog } from '../../../shared/components/ModalDialog'

type StatusFilter = 'ALL' | CourseStatus
type CreateForm = { title: string; code: string; description: string; startDate: string; endDate: string }
type ToastState = { message: string; tone: 'success' | 'error' }
const blankForm: CreateForm = { title: '', code: '', description: '', startDate: '', endDate: '' }

export function CourseLearning() {
  const user = getStoredUser()
  const canManage = user?.role === 'TEACHER' || user?.role === 'ADMIN'
  const [view, setView] = useState<'LEARNING' | 'MANAGEMENT'>(canManage ? 'MANAGEMENT' : 'LEARNING')

  if (user?.role === 'STUDENT') return <StudentClassrooms />
  if (!canManage) return <ManagedCourseLearning />

  return (
    <div>
      <div className="mb-5 flex justify-end">
        <div className="inline-flex rounded-lg border border-slate-200 bg-slate-100 p-1" role="tablist" aria-label="Chế độ lớp học">
          <button
            type="button"
            role="tab"
            aria-selected={view === 'LEARNING'}
            onClick={() => setView('LEARNING')}
            className={`cursor-pointer rounded-md px-3 py-2 text-xs font-semibold transition ${view === 'LEARNING' ? 'bg-white text-blue-700 shadow-sm' : 'text-slate-500 hover:text-slate-900'}`}
          >
            Học tập
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={view === 'MANAGEMENT'}
            onClick={() => setView('MANAGEMENT')}
            className={`cursor-pointer rounded-md px-3 py-2 text-xs font-semibold transition ${view === 'MANAGEMENT' ? 'bg-white text-blue-700 shadow-sm' : 'text-slate-500 hover:text-slate-900'}`}
          >
            Quản lý lớp
          </button>
        </div>
      </div>
      {view === 'LEARNING' ? <StudentClassrooms /> : <ManagedCourseLearning />}
    </div>
  )
}

function ManagedCourseLearning() {
  const user = getStoredUser()
  const canManage = user?.role === 'TEACHER' || user?.role === 'ADMIN'
  const [courses, setCourses] = useState<ManagedCourse[]>([])
  const [loading, setLoading] = useState(canManage)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState<StatusFilter>('ALL')
  const [current, setCurrent] = useState<ManagedCourse | null>(null)
  const [students, setStudents] = useState<CourseStudent[]>([])
  const [studentsLoading, setStudentsLoading] = useState(false)
  const [createOpen, setCreateOpen] = useState(false)
  const [editingCourse, setEditingCourse] = useState<ManagedCourse | null>(null)
  const [deletingCourse, setDeletingCourse] = useState<ManagedCourse | null>(null)
  const [deletingCourseId, setDeletingCourseId] = useState<string | null>(null)
  const [form, setForm] = useState<CreateForm>(blankForm)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')
  const [toast, setToast] = useState<ToastState>({ message: '', tone: 'success' })
  const isAdmin = user?.role === 'ADMIN'
  const [teacherFilter, setTeacherFilter] = useState('ALL')

  function showToast(message: string, tone: ToastState['tone'] = 'success') {
    setToast({ message, tone })
  }

  async function loadCourses() {
    if (!canManage) return
    setLoading(true); setError('')
    try { setCourses(await getManagedCourses()) }
    catch (reason) { setError(messageOf(reason, 'Không thể tải danh sách lớp học.')) }
    finally { setLoading(false) }
  }

  async function openCourse(course: ManagedCourse) {
    setCurrent(course); setStudentsLoading(true); setError('')
    try { setStudents(await getCourseStudents(course.id)) }
    catch (reason) { setError(messageOf(reason, 'Không thể tải danh sách sinh viên.')) }
    finally { setStudentsLoading(false) }
  }

  function applyRoster(roster: CourseStudent[]) {
    setStudents(roster)
    if (!current) return
    const updated = { ...current, studentCount: roster.length }
    setCurrent(updated)
    setCourses((items) => items.map((item) => item.id === updated.id ? updated : item))
  }

  async function submitCreate(event: FormEvent) {
    event.preventDefault()
    const code = form.code.trim().toUpperCase()
    if (!/^[A-Z0-9]+(?:-[A-Z0-9]+)*$/.test(code)) return setFormError('Mã lớp chỉ gồm chữ, số và dấu gạch ngang.')
    if (form.startDate && form.endDate && form.endDate < form.startDate) return setFormError('Ngày kết thúc không được trước ngày bắt đầu.')
    setSaving(true); setFormError('')
    try {
      await createCourse({ slug: code.toLowerCase(), title: form.title, description: form.description, startDate: form.startDate || null, endDate: form.endDate || null })
      await loadCourses(); setCreateOpen(false); showToast('Tạo lớp học thành công')
    } catch (reason) { const message = messageOf(reason, 'Không thể tạo lớp học.'); setFormError(message); showToast(message, 'error') }
    finally { setSaving(false) }
  }

  async function submitEdit(event: FormEvent) {
    event.preventDefault()
    if (!editingCourse) return
    const code = form.code.trim().toUpperCase()
    if (!/^[A-Z0-9]+(?:-[A-Z0-9]+)*$/.test(code)) return setFormError('Mã lớp chỉ gồm chữ, số và dấu gạch ngang.')
    if (form.startDate && form.endDate && form.endDate < form.startDate) return setFormError('Ngày kết thúc không được trước ngày bắt đầu.')
    setSaving(true); setFormError('')
    try {
      await updateManagedCourse(editingCourse.id, { slug: code.toLowerCase(), title: form.title, description: form.description, startDate: form.startDate || null, endDate: form.endDate || null })
      await loadCourses(); setEditingCourse(null); showToast('Đã cập nhật lớp học')
    } catch (reason) { const message = messageOf(reason, 'Không thể cập nhật lớp học.'); setFormError(message); showToast(message, 'error') }
    finally { setSaving(false) }
  }

  async function removeCourse() {
    if (!deletingCourse || deletingCourseId) return
    setDeletingCourseId(deletingCourse.id)
    try {
      await deleteManagedCourse(deletingCourse.id)
      setCourses((items) => items.filter((course) => course.id !== deletingCourse.id))
      if (current?.id === deletingCourse.id) setCurrent(null)
      setDeletingCourse(null); showToast('Đã xóa lớp học')
    } catch (reason) { showToast(messageOf(reason, 'Không thể xóa lớp học.'), 'error') }
    finally { setDeletingCourseId(null) }
  }

  useEffect(() => { void loadCourses() }, [])
  useEffect(() => { if (!toast.message) return; const timer = window.setTimeout(() => setToast((current) => ({ ...current, message: '' })), 2800); return () => clearTimeout(timer) }, [toast])

  const visibleCourses = useMemo(() => {
    const keyword = search.trim().toLocaleLowerCase('vi')
    return courses.filter((course) => (filter === 'ALL' || course.status === filter)
      && (teacherFilter === 'ALL' || course.teacherName === teacherFilter)
      && (!keyword || `${course.title} ${course.code} ${course.description} ${course.teacherName}`.toLocaleLowerCase('vi').includes(keyword)))
  }, [courses, filter, search, teacherFilter])

  const teacherNames = useMemo(() => Array.from(new Set(courses.map((course) => course.teacherName))).sort((left, right) => left.localeCompare(right, 'vi')), [courses])

  return <section className="pb-8" aria-labelledby="classroom-title">
    {current ? <ClassStudentManagement course={current} students={students} loading={studentsLoading} error={error} onBack={() => { setCurrent(null); setError('') }} onRoster={applyRoster} onToast={showToast} onRetry={() => void openCourse(current)} /> : <>
      <PageHeader disabled={!canManage} onCreate={() => { setForm(blankForm); setFormError(''); setCreateOpen(true) }} />
      <div className="mt-5 flex flex-col gap-3 border-y border-slate-200 py-3 sm:flex-row sm:items-center sm:justify-between">
        <ClassSearch value={search} onChange={setSearch} />
        <div className="flex gap-1 overflow-x-auto rounded-md bg-slate-100 p-1">{([['ALL', 'Tất cả'], ['ACTIVE', 'Đang hoạt động'], ['ENDED', 'Đã kết thúc']] as const).map(([value, label]) => <button key={value} type="button" onClick={() => setFilter(value)} className={`shrink-0 cursor-pointer rounded px-3 py-1.5 text-xs font-medium transition-colors ${filter === value ? 'bg-white text-blue-600 shadow-sm' : 'text-slate-500 hover:text-slate-900'}`}>{label}</button>)}</div>
        {isAdmin ? <label className="flex shrink-0 items-center gap-2 text-xs font-semibold text-slate-600"><span>Giảng viên</span><select value={teacherFilter} onChange={(event) => setTeacherFilter(event.target.value)} className="ui-control min-h-9 w-auto py-1.5 text-xs font-semibold"><option value="ALL">Tất cả giáo viên</option>{teacherNames.map((teacher) => <option key={teacher} value={teacher}>{teacher}</option>)}</select></label> : null}
      </div>
      {!canManage ? <Empty title="Trang dành cho giảng viên" text="Đăng nhập bằng tài khoản giáo viên hoặc quản trị viên để quản lý lớp học." /> : loading ? <CardsLoading /> : error ? <ErrorState text={error} onRetry={() => void loadCourses()} /> : visibleCourses.length === 0 ? <Empty title={courses.length ? 'Không tìm thấy lớp học' : 'Chưa có lớp học nào'} text={courses.length ? 'Thử thay đổi từ khóa hoặc bộ lọc trạng thái.' : 'Tạo lớp học đầu tiên để bắt đầu quản lý sinh viên.'} /> : <div className="mt-5 grid gap-4 md:grid-cols-2 xl:grid-cols-3">{visibleCourses.map((course) => <CourseCard key={course.id} course={course} onOpen={() => void openCourse(course)} onEdit={isAdmin ? () => { setForm({ title: course.title, code: course.code, description: course.description, startDate: course.startDate, endDate: course.endDate ?? '' }); setFormError(''); setEditingCourse(course) } : undefined} onDelete={isAdmin ? () => setDeletingCourse(course) : undefined} />)}</div>}
    </>}
    {createOpen ? <CreateClassModal form={form} setForm={setForm} error={formError} saving={saving} onClose={() => !saving && setCreateOpen(false)} onSubmit={submitCreate} /> : null}
    {editingCourse ? <CreateClassModal mode="edit" form={form} setForm={setForm} error={formError} saving={saving} onClose={() => !saving && setEditingCourse(null)} onSubmit={submitEdit} /> : null}
    {deletingCourse ? <ModalDialog title="Xóa lớp học?" onClose={() => !deletingCourseId && setDeletingCourse(null)}><div className="space-y-5 p-5 sm:p-6"><p className="text-sm leading-6 text-slate-600">Bạn có chắc chắn muốn xóa lớp <strong className="text-slate-900">{deletingCourse.title}</strong>? Danh sách sinh viên, bài tập được gán và tài liệu của lớp cũng sẽ bị xóa.</p><div className="flex flex-col-reverse gap-3 border-t border-slate-200 pt-5 sm:flex-row sm:justify-end"><button type="button" disabled={Boolean(deletingCourseId)} onClick={() => setDeletingCourse(null)} className="ui-button-secondary">Hủy</button><button type="button" disabled={Boolean(deletingCourseId)} onClick={() => void removeCourse()} className="min-h-10 rounded-xl bg-red-600 px-4 text-sm font-bold text-white transition hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-60">{deletingCourseId ? 'Đang xóa...' : 'Xóa lớp học'}</button></div></div></ModalDialog> : null}
    <Toast {...toast} />
  </section>
}

function PageHeader({ disabled, onCreate }: { disabled: boolean; onCreate: () => void }) { return <header className="ui-page-header"><div><div className="ui-kicker"><IconBookOpen className="h-4 w-4" />Không gian giảng dạy</div><h1 id="classroom-title" className="ui-page-title mt-2">Lớp học</h1><p className="ui-page-description">Quản lý lớp học và sinh viên của bạn.</p></div><button type="button" disabled={disabled} onClick={onCreate} className="ui-button-primary w-full sm:w-auto">+ Tạo lớp học</button></header> }
function CourseCard({ course, onOpen, onEdit, onDelete }: { course: ManagedCourse; onOpen: () => void; onEdit?: () => void; onDelete?: () => void }) { return <article className="group ui-card ui-card-interactive flex min-h-60 flex-col p-4 sm:p-5"><div className="flex justify-between gap-3"><span className="rounded bg-slate-100 px-2 py-1 font-mono text-[10px] font-semibold text-slate-600">{course.code}</span><StatusBadge status={course.status} /></div><h2 className="mt-4 text-base font-semibold text-slate-900">{course.title}</h2><p className="mt-1.5 line-clamp-2 min-h-10 text-xs leading-5 text-slate-500">{course.description || 'Chưa có mô tả cho lớp học này.'}</p><p className="mt-2 text-xs font-medium text-slate-500">Giảng viên: <span className="text-slate-700">{course.teacherName}</span></p><div className="mt-4 grid grid-cols-2 gap-3 border-t border-slate-100 pt-3 text-xs text-slate-500"><span className="flex items-center gap-2"><IconUser className="h-3.5 w-3.5 text-blue-600" /><b>{course.studentCount}</b> sinh viên</span><span className="flex items-center justify-end gap-2"><IconCalendar className="h-3.5 w-3.5 text-blue-600" />{formatCourseDate(course.startDate)}</span></div><div className="mt-auto grid gap-2 pt-4"><button type="button" onClick={onOpen} className="ui-button-secondary w-full">Quản lý lớp<IconArrowRight className="h-4 w-4" /></button>{onEdit || onDelete ? <div className="grid grid-cols-2 gap-2"><button type="button" onClick={onEdit} className="min-h-9 cursor-pointer rounded-lg border border-slate-200 bg-white px-3 text-xs font-bold text-slate-700 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700">Sửa lớp</button><button type="button" onClick={onDelete} className="min-h-9 cursor-pointer rounded-lg border border-red-200 bg-white px-3 text-xs font-bold text-red-700 transition hover:bg-red-50">Xóa lớp</button></div> : null}</div></article> }
function CreateClassModal({ form, setForm, error, saving, onClose, onSubmit, mode = 'create' }: { form: CreateForm; setForm: (form: CreateForm) => void; error: string; saving: boolean; onClose: () => void; onSubmit: (event: FormEvent) => void; mode?: 'create' | 'edit' }) { const field = (key: keyof CreateForm, value: string) => setForm({ ...form, [key]: value }); const editing = mode === 'edit'; return <ModalDialog title={editing ? 'Sửa lớp học' : 'Tạo lớp học'} onClose={onClose}><form onSubmit={(event) => void onSubmit(event)} className="space-y-4 p-5 sm:p-6"><div className="grid gap-4 sm:grid-cols-2"><Input label="Tên lớp học *" value={form.title} onChange={(value) => field('title', value)} /><Input label="Mã lớp *" value={form.code} onChange={(value) => field('code', value.toUpperCase())} /></div><label className="block text-sm font-semibold text-slate-700">Mô tả<textarea value={form.description} onChange={(event) => field('description', event.target.value)} rows={3} className="ui-control mt-2 font-normal" /></label><div className="grid gap-4 sm:grid-cols-2"><Input label="Ngày bắt đầu" type="date" required={false} value={form.startDate} onChange={(value) => field('startDate', value)} /><Input label="Ngày kết thúc" type="date" required={false} value={form.endDate} onChange={(value) => field('endDate', value)} /></div>{error ? <p className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm font-semibold text-red-700">{error}</p> : null}<div className="flex flex-col-reverse gap-3 border-t border-slate-200 pt-5 sm:flex-row sm:justify-end"><button type="button" onClick={onClose} disabled={saving} className="ui-button-secondary">Hủy</button><button type="submit" disabled={saving} className="ui-button-primary">{saving ? (editing ? 'Đang lưu...' : 'Đang tạo...') : (editing ? 'Lưu thay đổi' : 'Tạo lớp học')}</button></div></form></ModalDialog> }
function ClassSearch({ value, onChange }: { value: string; onChange: (value: string) => void }) { return <label className="relative block flex-1 sm:max-w-md"><span className="sr-only">Tìm kiếm lớp học</span><span className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400">⌕</span><input type="search" value={value} onChange={(event) => onChange(event.target.value)} placeholder="Tìm kiếm lớp học..." className="ui-control ui-control-with-leading-icon" /></label> }
function Input({ label, value, onChange, type = 'text', required = true }: { label: string; value: string; onChange: (value: string) => void; type?: string; required?: boolean }) { return <label className="block text-sm font-semibold text-slate-700">{label}<input type={type} required={required} value={value} onChange={(event) => onChange(event.target.value)} className="ui-control mt-2 font-normal" /></label> }
function StatusBadge({ status }: { status: CourseStatus }) { return <span className={`rounded px-2 py-1 text-[10px] font-semibold ${status === 'ACTIVE' ? 'bg-blue-50 text-blue-700' : 'bg-slate-100 text-slate-600'}`}>{status === 'ACTIVE' ? 'Đang hoạt động' : 'Đã kết thúc'}</span> }
function Empty({ title, text }: { title: string; text: string }) { return <div className="ui-state mt-6"><div><IconBookOpen className="mx-auto h-7 w-7 text-blue-600" /><h2 className="mt-3 text-lg font-bold text-slate-900">{title}</h2><p className="mt-2 text-sm text-slate-500">{text}</p></div></div> }
function ErrorState({ text, onRetry }: { text: string; onRetry: () => void }) { return <div className="mt-6 rounded-2xl border border-red-200 bg-red-50 p-6 text-center"><p className="text-sm font-semibold text-red-700">{text}</p><button type="button" onClick={onRetry} className="mt-4 min-h-10 cursor-pointer rounded-xl border border-red-200 bg-white px-4 text-sm font-bold text-red-700">Thử lại</button></div> }
function CardsLoading() { return <div className="mt-5 grid gap-4 md:grid-cols-2 xl:grid-cols-3">{[1, 2, 3].map((item) => <div key={item} className="ui-skeleton h-60 rounded-lg" />)}</div> }
function Toast({ message, tone }: ToastState) { const Icon = tone === 'error' ? IconHelpCircle : IconCheckCircle; return <div aria-live="polite" className={`fixed bottom-5 right-5 z-[90] max-w-[calc(100vw-2.5rem)] transition-all duration-300 ${message ? 'translate-y-0 opacity-100' : 'pointer-events-none translate-y-4 opacity-0'}`}><div className={`flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-bold text-white shadow-xl ${tone === 'error' ? 'bg-red-600' : 'bg-blue-700'}`}><Icon className="h-5 w-5 shrink-0" />{message}</div></div> }
function formatCourseDate(value: string) { const [year, month, day] = value.split('-'); return year && month && day ? `${day}/${month}/${year}` : value }
function messageOf(reason: unknown, fallback: string) { if (!(reason instanceof globalThis.Error)) return fallback; if (reason.message === 'AUTHENTICATION_REQUIRED') return 'Bạn cần đăng nhập để quản lý lớp học.'; if (reason.message === 'ROLE_REQUIRED') return 'Chỉ giáo viên hoặc quản trị viên được quản lý lớp học.'; return reason.message || fallback }
