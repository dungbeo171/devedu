import { useEffect, useMemo, useState } from 'react'
import { getTeacherCourseStudentProgress } from '../api/courseLearningApi'
import type { CourseStudentProgress, ManagedCourse } from '../types/courseLearning'

export function TeacherStudentProgress({ course }: { course: ManagedCourse }) {
  const [students, setStudents] = useState<CourseStudentProgress[]>([])
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  async function load() {
    setLoading(true)
    setError('')
    try {
      setStudents(await getTeacherCourseStudentProgress(course.id))
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Không thể tải tiến trình sinh viên.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void load() }, [course.id])

  const visibleStudents = useMemo(() => {
    const keyword = query.trim().toLocaleLowerCase('vi')
    return students.filter((student) => !keyword
      || `${student.name} ${student.email} ${student.studentCode}`.toLocaleLowerCase('vi').includes(keyword))
  }, [query, students])

  const completedCount = students.filter((student) => student.totalProblems > 0
    && student.solvedProblems === student.totalProblems).length
  const averageProgress = students.length === 0
    ? 0
    : Math.round(students.reduce((total, student) => total + student.progressPercent, 0) / students.length)

  if (loading) return <div className="mt-6 space-y-3">{[1, 2, 3, 4].map((item) => <div key={item} className="ui-skeleton h-20 rounded-xl" />)}</div>
  if (error) return <div role="alert" className="mt-6 rounded-xl border border-red-200 bg-red-50 p-6 text-center text-sm font-semibold text-red-700">{error}<button type="button" onClick={() => void load()} className="ui-button-danger mx-auto mt-4 flex">Thử lại</button></div>

  return (
    <section className="mt-6" aria-label="Tiến trình sinh viên">
      <div className="grid grid-flow-dense gap-3 sm:grid-cols-3">
        <Metric label="Sinh viên" value={students.length} />
        <Metric label="Tiến trình trung bình" value={`${averageProgress}%`} />
        <Metric label="Đã hoàn thành" value={completedCount} />
      </div>

      <div className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-xl font-bold text-slate-950">Tiến trình bài tập</h2>
          <p className="mt-1 text-sm text-slate-500">Theo dõi số bài đã giải của từng sinh viên trong lớp.</p>
        </div>
        <input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm sinh viên..." className="ui-control sm:max-w-xs" />
      </div>

      {students.length === 0 ? (
        <div className="ui-state mt-5"><div><h3 className="text-lg font-bold text-slate-900">Chưa có dữ liệu tiến trình</h3><p className="mt-2 text-sm">Thêm sinh viên và giao bài tập để bắt đầu theo dõi.</p></div></div>
      ) : visibleStudents.length === 0 ? (
        <div className="ui-state mt-5 min-h-36 text-sm">Không tìm thấy sinh viên phù hợp.</div>
      ) : (
        <div className="ui-panel mt-5 overflow-hidden">
          <div className="hidden grid-cols-[minmax(220px,1fr)_120px_minmax(240px,1.2fr)_90px] gap-4 border-b border-slate-200 bg-slate-50 px-5 py-3 text-xs font-bold text-slate-600 md:grid">
            <span>Sinh viên</span><span>Mã sinh viên</span><span>Tiến trình</span><span className="text-right">Hoàn thành</span>
          </div>
          <div className="divide-y divide-slate-100">
            {visibleStudents.map((student) => (
              <article key={student.id} className="grid gap-4 px-5 py-4 transition hover:bg-blue-50/35 md:grid-cols-[minmax(220px,1fr)_120px_minmax(240px,1.2fr)_90px] md:items-center">
                <div className="flex min-w-0 items-center gap-3">
                  <span className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-blue-600 text-xs font-bold text-white">{initials(student.name)}</span>
                  <span className="min-w-0"><b className="block truncate text-sm text-slate-950">{student.name}</b><span className="block truncate text-xs text-slate-500">{student.email}</span></span>
                </div>
                <code className="text-xs font-bold text-blue-700">{student.studentCode}</code>
                <div>
                  <div className="mb-2 flex justify-between text-xs font-semibold text-slate-600"><span>{student.solvedProblems}/{student.totalProblems} bài</span><span className="text-blue-700">{student.progressPercent}%</span></div>
                  <div className="h-2 overflow-hidden rounded-full bg-blue-100"><div className="h-full rounded-full bg-blue-600 transition-[width] duration-500" style={{ width: `${student.progressPercent}%` }} /></div>
                </div>
                <span className="text-left text-xs font-bold text-slate-600 md:text-right">{student.totalProblems > 0 && student.solvedProblems === student.totalProblems ? 'Đã xong' : 'Đang học'}</span>
              </article>
            ))}
          </div>
        </div>
      )}
    </section>
  )
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return <div className="ui-card p-4"><p className="text-xs font-semibold text-slate-500">{label}</p><p className="mt-1 text-2xl font-bold text-slate-950">{value}</p></div>
}

function initials(name: string) {
  return name.split(/\s+/).slice(-2).map((part) => part[0]).join('').toLocaleUpperCase('vi')
}
