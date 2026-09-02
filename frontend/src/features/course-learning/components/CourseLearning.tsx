import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from 'react'
import { createCourse, getCourseStudents, getManagedCourses } from '../api/courseLearningApi'
import type { CourseStatus, CourseStudent, ManagedCourse } from '../types/courseLearning'
import { getStoredUser } from '../../auth/api/authApi'
import { IconArrowRight, IconBookOpen, IconCalendar, IconCheckCircle, IconUser } from '../../../shared/components/Icons'
import { ClassStudentManagement } from './ClassStudentManagement'
import { StudentClassrooms } from './StudentClassrooms'

type StatusFilter = 'ALL' | CourseStatus
type CreateForm = { title: string; code: string; description: string; startDate: string; endDate: string }
const blankForm: CreateForm = { title: '', code: '', description: '', startDate: '', endDate: '' }

export function CourseLearning() {
  const user = getStoredUser()
  return user?.role === 'STUDENT' ? <StudentClassrooms /> : <ManagedCourseLearning />
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
  const [form, setForm] = useState<CreateForm>(blankForm)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')
  const [toast, setToast] = useState('')

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
      await loadCourses(); setCreateOpen(false); setToast('Tạo lớp học thành công')
    } catch (reason) { setFormError(messageOf(reason, 'Không thể tạo lớp học.')) }
    finally { setSaving(false) }
  }

  useEffect(() => { void loadCourses() }, [])
  useEffect(() => { if (!toast) return; const timer = window.setTimeout(() => setToast(''), 2800); return () => clearTimeout(timer) }, [toast])

  const visibleCourses = useMemo(() => {
    const keyword = search.trim().toLocaleLowerCase('vi')
    return courses.filter((course) => (filter === 'ALL' || course.status === filter)
      && (!keyword || `${course.title} ${course.code} ${course.description}`.toLocaleLowerCase('vi').includes(keyword)))
  }, [courses, filter, search])

  return <section className="pb-8" aria-labelledby="classroom-title">
    {current ? <ClassStudentManagement course={current} students={students} loading={studentsLoading} error={error} onBack={() => { setCurrent(null); setError('') }} onRoster={applyRoster} onToast={setToast} onRetry={() => void openCourse(current)} /> : <>
      <PageHeader disabled={!canManage} onCreate={() => { setForm(blankForm); setFormError(''); setCreateOpen(true) }} />
      <div className="mt-7 flex flex-col gap-4 rounded-2xl border border-slate-200 bg-white p-3 shadow-sm sm:flex-row sm:items-center sm:justify-between">
        <ClassSearch value={search} onChange={setSearch} />
        <div className="flex gap-1 overflow-x-auto rounded-xl bg-blue-50 p-1">{([['ALL', 'Tất cả'], ['ACTIVE', 'Đang hoạt động'], ['ENDED', 'Đã kết thúc']] as const).map(([value, label]) => <button key={value} type="button" onClick={() => setFilter(value)} className={`shrink-0 cursor-pointer rounded-lg px-3.5 py-2 text-xs font-bold ${filter === value ? 'bg-blue-600 text-white' : 'text-blue-700 hover:bg-white'}`}>{label}</button>)}</div>
      </div>
      {!canManage ? <Empty title="Trang dành cho giảng viên" text="Đăng nhập bằng tài khoản giáo viên hoặc quản trị viên để quản lý lớp học." /> : loading ? <CardsLoading /> : error ? <ErrorState text={error} onRetry={() => void loadCourses()} /> : visibleCourses.length === 0 ? <Empty title={courses.length ? 'Không tìm thấy lớp học' : 'Chưa có lớp học nào'} text={courses.length ? 'Thử thay đổi từ khóa hoặc bộ lọc trạng thái.' : 'Tạo lớp học đầu tiên để bắt đầu quản lý sinh viên.'} /> : <div className="mt-6 grid gap-5 md:grid-cols-2 xl:grid-cols-3">{visibleCourses.map((course) => <CourseCard key={course.id} course={course} onOpen={() => void openCourse(course)} />)}</div>}
    </>}
    {createOpen ? <CreateClassModal form={form} setForm={setForm} error={formError} saving={saving} onClose={() => !saving && setCreateOpen(false)} onSubmit={submitCreate} /> : null}
    <Toast message={toast} />
  </section>
}

function PageHeader({ disabled, onCreate }: { disabled: boolean; onCreate: () => void }) { return <header className="rounded-3xl border border-blue-100 bg-gradient-to-br from-blue-50 via-white to-white px-5 py-6 shadow-sm sm:px-8 sm:py-8"><div className="flex flex-col gap-6 md:flex-row md:items-end md:justify-between"><div><div className="inline-flex items-center gap-2 text-xs font-extrabold uppercase tracking-[.18em] text-blue-600"><span className="grid h-8 w-8 place-items-center rounded-xl bg-blue-600 text-white"><IconBookOpen className="h-4 w-4" /></span>DevEdu</div><h1 id="classroom-title" className="mt-4 text-3xl font-black text-slate-950 sm:text-4xl">Lớp học</h1><p className="mt-2 text-sm text-slate-600">Quản lý lớp học và sinh viên của bạn.</p></div><button type="button" disabled={disabled} onClick={onCreate} className="min-h-11 w-full cursor-pointer rounded-xl bg-blue-600 px-5 text-sm font-bold text-white shadow-lg shadow-blue-200 hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50 sm:w-auto">+ Tạo lớp học</button></div></header> }
function CourseCard({ course, onOpen }: { course: ManagedCourse; onOpen: () => void }) { return <article className="group flex min-h-72 flex-col rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:border-blue-200 hover:shadow-xl sm:p-6"><div className="flex justify-between gap-3"><span className="rounded-lg bg-blue-50 px-2.5 py-1 font-mono text-[11px] font-extrabold text-blue-700">{course.code}</span><StatusBadge status={course.status} /></div><h2 className="mt-5 text-lg font-extrabold">{course.title}</h2><p className="mt-2 line-clamp-2 min-h-10 text-sm text-slate-500">{course.description || 'Chưa có mô tả cho lớp học này.'}</p><div className="mt-5 grid grid-cols-2 gap-3 border-t border-slate-100 pt-4 text-sm text-slate-600"><span className="flex items-center gap-2"><IconUser className="h-4 w-4 text-blue-600" /><b>{course.studentCount}</b> sinh viên</span><span className="flex items-center justify-end gap-2"><IconCalendar className="h-4 w-4 text-blue-600" />{formatCourseDate(course.startDate)}</span></div><button type="button" onClick={onOpen} className="mt-auto flex w-full cursor-pointer items-center justify-center gap-2 rounded-xl border border-blue-200 px-4 py-2.5 text-sm font-bold text-blue-700 hover:bg-blue-600 hover:text-white">Quản lý lớp<IconArrowRight className="h-4 w-4" /></button></article> }
function CreateClassModal({ form, setForm, error, saving, onClose, onSubmit }: { form: CreateForm; setForm: (form: CreateForm) => void; error: string; saving: boolean; onClose: () => void; onSubmit: (event: FormEvent) => void }) { const field = (key: keyof CreateForm, value: string) => setForm({ ...form, [key]: value }); return <Modal title="Tạo lớp học" onClose={onClose}><form onSubmit={(event) => void onSubmit(event)} className="space-y-4 p-5 sm:p-6"><div className="grid gap-4 sm:grid-cols-2"><Input label="Tên lớp học *" value={form.title} onChange={(value) => field('title', value)} /><Input label="Mã lớp *" value={form.code} onChange={(value) => field('code', value.toUpperCase())} /></div><label className="block text-sm font-bold">Mô tả<textarea value={form.description} onChange={(event) => field('description', event.target.value)} rows={3} className="mt-2 w-full rounded-xl border border-slate-200 p-3 font-normal outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-50" /></label><div className="grid gap-4 sm:grid-cols-2"><Input label="Ngày bắt đầu" type="date" required={false} value={form.startDate} onChange={(value) => field('startDate', value)} /><Input label="Ngày kết thúc" type="date" required={false} value={form.endDate} onChange={(value) => field('endDate', value)} /></div>{error ? <p className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm font-semibold text-red-700">{error}</p> : null}<div className="flex flex-col-reverse gap-3 border-t pt-5 sm:flex-row sm:justify-end"><button type="button" onClick={onClose} disabled={saving} className="min-h-11 cursor-pointer rounded-xl border border-slate-200 px-5 text-sm font-bold hover:bg-slate-50 disabled:opacity-50">Hủy</button><button type="submit" disabled={saving} className="min-h-11 cursor-pointer rounded-xl bg-blue-600 px-5 text-sm font-bold text-white hover:bg-blue-700 disabled:cursor-wait disabled:opacity-60">{saving ? 'Đang tạo...' : 'Tạo lớp học'}</button></div></form></Modal> }
function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) { return <div className="fixed inset-0 z-[80] flex items-end justify-center bg-slate-950/45 p-0 backdrop-blur-[2px] sm:items-center sm:p-4" role="dialog" aria-modal="true" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><div className="max-h-[95vh] w-full overflow-y-auto rounded-t-3xl bg-white shadow-2xl sm:max-w-xl sm:rounded-3xl"><div className="flex items-center justify-between border-b border-slate-100 px-5 py-4"><div><p className="text-xs font-extrabold uppercase tracking-[.16em] text-blue-600">DevEdu</p><h2 className="mt-1 text-xl font-black">{title}</h2></div><button type="button" onClick={onClose} aria-label="Đóng" className="grid h-9 w-9 cursor-pointer place-items-center rounded-full text-xl hover:bg-blue-50">×</button></div>{children}</div></div> }
function ClassSearch({ value, onChange }: { value: string; onChange: (value: string) => void }) { return <label className="relative block flex-1 sm:max-w-md"><span className="sr-only">Tìm kiếm lớp học</span><span className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400">⌕</span><input type="search" value={value} onChange={(event) => onChange(event.target.value)} placeholder="Tìm kiếm lớp học..." className="h-11 w-full rounded-xl border border-slate-200 bg-slate-50 pl-10 pr-4 text-sm outline-none focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-50" /></label> }
function Input({ label, value, onChange, type = 'text', required = true }: { label: string; value: string; onChange: (value: string) => void; type?: string; required?: boolean }) { return <label className="block text-sm font-bold">{label}<input type={type} required={required} value={value} onChange={(event) => onChange(event.target.value)} className="mt-2 h-11 w-full rounded-xl border border-slate-200 px-3.5 font-normal outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-50" /></label> }
function StatusBadge({ status }: { status: CourseStatus }) { return <span className={`rounded-full px-2.5 py-1 text-[11px] font-extrabold ${status === 'ACTIVE' ? 'bg-blue-50 text-blue-700 ring-1 ring-blue-100' : 'bg-slate-100 text-slate-600'}`}>{status === 'ACTIVE' ? 'Đang hoạt động' : 'Đã kết thúc'}</span> }
function Empty({ title, text }: { title: string; text: string }) { return <div className="mt-6 rounded-3xl border border-dashed border-blue-200 bg-blue-50/40 px-5 py-12 text-center"><IconBookOpen className="mx-auto h-7 w-7 text-blue-600" /><h2 className="mt-3 font-black">{title}</h2><p className="mt-2 text-sm text-slate-500">{text}</p></div> }
function ErrorState({ text, onRetry }: { text: string; onRetry: () => void }) { return <div className="mt-6 rounded-2xl border border-red-200 bg-red-50 p-6 text-center"><p className="text-sm font-semibold text-red-700">{text}</p><button type="button" onClick={onRetry} className="mt-4 min-h-10 cursor-pointer rounded-xl border border-red-200 bg-white px-4 text-sm font-bold text-red-700">Thử lại</button></div> }
function CardsLoading() { return <div className="mt-6 grid gap-5 md:grid-cols-2 xl:grid-cols-3">{[1, 2, 3].map((item) => <div key={item} className="h-72 animate-pulse rounded-2xl bg-blue-50" />)}</div> }
function Toast({ message }: { message: string }) { return <div aria-live="polite" className={`fixed bottom-5 right-5 z-[90] max-w-[calc(100vw-2.5rem)] transition-all duration-300 ${message ? 'translate-y-0 opacity-100' : 'pointer-events-none translate-y-4 opacity-0'}`}><div className="flex items-center gap-3 rounded-xl bg-blue-700 px-4 py-3 text-sm font-bold text-white shadow-2xl"><IconCheckCircle className="h-5 w-5 shrink-0" />{message}</div></div> }
function formatCourseDate(value: string) { const [year, month, day] = value.split('-'); return year && month && day ? `${day}/${month}/${year}` : value }
function messageOf(reason: unknown, fallback: string) { if (!(reason instanceof globalThis.Error)) return fallback; if (reason.message === 'AUTHENTICATION_REQUIRED') return 'Bạn cần đăng nhập để quản lý lớp học.'; if (reason.message === 'ROLE_REQUIRED') return 'Chỉ giáo viên hoặc quản trị viên được quản lý lớp học.'; return reason.message || fallback }
