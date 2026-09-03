import { useEffect, useState } from 'react'
import { getStudentCourse, getStudentCourses } from '../api/courseLearningApi'
import type { StudentCourse, StudentCourseDetails } from '../types/courseLearning'
import { difficultyLabels, topicLabels } from './studentCourseLabels'

export function StudentClassrooms() {
  const [courses, setCourses] = useState<StudentCourse[]>([])
  const [current, setCurrent] = useState<StudentCourseDetails | null>(null)
  const [tab, setTab] = useState<'PROBLEMS' | 'PROGRESS'>('PROBLEMS')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  async function loadCourses() {
    setLoading(true); setError('')
    try { setCourses(await getStudentCourses()) }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Không thể tải lớp học của bạn.') }
    finally { setLoading(false) }
  }
  async function openCourse(courseId: string) {
    setLoading(true); setError('')
    try { setCurrent(await getStudentCourse(courseId)); setTab('PROBLEMS') }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Không thể tải lớp học.') }
    finally { setLoading(false) }
  }
  useEffect(() => { void loadCourses() }, [])

  if (current) return <section>
    <button type="button" onClick={() => { setCurrent(null); void loadCourses() }} className="mb-4 cursor-pointer text-sm font-bold text-blue-700">← Lớp học của tôi</button>
    <header className="ui-panel border-l-4 border-l-blue-600 p-5 sm:p-8"><p className="font-mono text-xs font-extrabold text-blue-700">{current.course.code}</p><h1 className="mt-2 text-3xl font-bold text-slate-950">{current.course.title}</h1><p className="mt-2 text-sm text-slate-600">Giảng viên: <b>{current.course.teacherName}</b></p><ProgressBar progress={current.course.progressPercent} solved={current.course.solvedProblems} total={current.course.totalProblems} /></header>
    <nav className="mt-6 flex gap-1 border-b border-slate-200"><button type="button" onClick={() => setTab('PROBLEMS')} className={`cursor-pointer border-b-2 px-4 py-3 text-sm font-bold ${tab === 'PROBLEMS' ? 'border-blue-600 text-blue-700' : 'border-transparent text-slate-500'}`}>Bài tập</button><button type="button" onClick={() => setTab('PROGRESS')} className={`cursor-pointer border-b-2 px-4 py-3 text-sm font-bold ${tab === 'PROGRESS' ? 'border-blue-600 text-blue-700' : 'border-transparent text-slate-500'}`}>Tiến trình</button></nav>
    {tab === 'PROGRESS' ? <div className="ui-panel mt-6 p-6"><h2 className="text-xl font-bold">Tiến trình học tập</h2><ProgressBar progress={current.course.progressPercent} solved={current.course.solvedProblems} total={current.course.totalProblems} large /><div className="mt-6 grid grid-flow-dense gap-3 sm:grid-cols-2">{current.problems.map((problem) => <div key={problem.id} className="flex items-center gap-3 rounded-xl border border-slate-200 p-4"><span className={`grid h-8 w-8 place-items-center rounded-full font-black ${problem.solved ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-400'}`}>{problem.solved ? '✓' : '○'}</span><span className="min-w-0"><b className="block truncate text-sm">{problem.title}</b><span className="text-xs text-slate-500">{problem.solved ? 'Đã hoàn thành' : 'Chưa hoàn thành'}</span></span></div>)}</div></div> : <ProblemList details={current} />}
  </section>

  return <section><header className="ui-page-header"><div><p className="ui-kicker">Không gian học tập</p><h1 className="ui-page-title mt-2">Lớp học của tôi</h1><p className="ui-page-description">Xem bài tập và theo dõi tiến trình trong các lớp bạn đã tham gia.</p></div></header>{loading ? <div className="mt-6 grid gap-5 md:grid-cols-2">{[1, 2].map((item) => <div key={item} className="ui-skeleton h-60 rounded-[14px]" />)}</div> : error ? <ErrorState message={error} retry={() => void loadCourses()} /> : courses.length === 0 ? <div className="ui-state mt-6"><div><h2 className="text-lg font-bold text-slate-900">Bạn chưa tham gia lớp học nào</h2><p className="mt-2 text-sm text-slate-500">Giảng viên sẽ thêm bạn vào lớp bằng mã sinh viên.</p></div></div> : <div className="mt-6 grid grid-flow-dense gap-5 md:grid-cols-2 xl:grid-cols-3">{courses.map((course) => <article key={course.id} className="ui-card ui-card-interactive flex flex-col p-5"><div className="flex justify-between gap-3"><code className="text-xs font-bold text-blue-700">{course.code}</code><span className="ui-badge">{course.status === 'ACTIVE' ? 'Đang hoạt động' : 'Đã kết thúc'}</span></div><h2 className="mt-4 text-xl font-bold">{course.title}</h2><p className="mt-1 text-sm text-slate-500">Giảng viên: {course.teacherName}</p><ProgressBar progress={course.progressPercent} solved={course.solvedProblems} total={course.totalProblems} /><button type="button" onClick={() => void openCourse(course.id)} className="ui-button-primary mt-5">Vào lớp học</button></article>)}</div>}</section>
}

function ProblemList({ details }: { details: StudentCourseDetails }) { return <section className="mt-6"><div className="mb-4"><h2 className="text-xl font-bold">Bài tập lập trình</h2><p className="mt-1 text-sm text-slate-500">Hoàn thành các bài được giảng viên giao.</p></div>{details.problems.length === 0 ? <div className="ui-state"><div><h3 className="text-lg font-bold text-slate-900">Chưa có bài tập</h3><p className="mt-2 text-sm text-slate-500">Giảng viên chưa giao bài tập cho lớp.</p></div></div> : <div className="space-y-3">{details.problems.map((problem, index) => <article key={problem.id} className="ui-card flex flex-col gap-4 p-4 sm:flex-row sm:items-center"><span className={`grid h-10 w-10 shrink-0 place-items-center rounded-lg font-black ${problem.solved ? 'bg-blue-600 text-white' : 'bg-blue-50 text-blue-700'}`}>{problem.solved ? '✓' : index + 1}</span><div className="min-w-0 flex-1"><h3 className="font-bold">{problem.title}</h3><p className="mt-1 line-clamp-1 text-sm text-slate-500">{problem.summary}</p><div className="mt-2 flex gap-2 text-xs"><span className="ui-badge">{topicLabels[problem.topic]}</span><span className="rounded-full bg-slate-100 px-2 py-1 font-bold text-slate-600">{difficultyLabels[problem.difficulty]}</span></div></div><a href={`/problems/${encodeURIComponent(problem.slug)}`} className="ui-button-primary">{problem.solved ? 'Xem lại' : 'Làm bài'}</a></article>)}</div>}</section> }
function ProgressBar({ progress, solved, total, large = false }: { progress: number; solved: number; total: number; large?: boolean }) { return <div className={large ? 'mt-6' : 'mt-5'}><div className="mb-2 flex items-center justify-between text-sm"><span className="font-bold text-slate-700">{solved}/{total} bài đã hoàn thành</span><b className="text-blue-700">{progress}%</b></div><div className={`overflow-hidden rounded-full bg-blue-100 ${large ? 'h-4' : 'h-2.5'}`}><div className="h-full rounded-full bg-blue-600 transition-all" style={{ width: `${progress}%` }} /></div></div> }
function ErrorState({ message, retry }: { message: string; retry: () => void }) { return <div className="mt-6 rounded-xl border border-red-200 bg-red-50 p-6 text-center text-sm font-semibold text-red-700">{message}<button type="button" onClick={retry} className="ui-button-danger mx-auto mt-4 flex">Thử lại</button></div> }
