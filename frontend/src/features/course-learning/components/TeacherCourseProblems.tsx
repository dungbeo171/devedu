import { useEffect, useMemo, useState } from 'react'
import { getProgrammingProblems } from '../../programming-problems/api/programmingProblemsApi'
import type { ProgrammingProblemSummary } from '../../programming-problems/types/programmingProblem'
import { topicLabels } from '../../programming-problems/types/programmingProblem'
import { ModalDialog } from '../../../shared/components/ModalDialog'
import { assignTeacherCourseProblem, getTeacherCourseProblems, removeTeacherCourseProblem } from '../api/courseLearningApi'
import type { CourseProblem, ManagedCourse } from '../types/courseLearning'

const difficultyLabels = { EASY: 'Dễ', MEDIUM: 'Trung bình', HARD: 'Khó' } as const

export function TeacherCourseProblems({ course, onToast }: { course: ManagedCourse; onToast: (message: string) => void }) {
  const [assigned, setAssigned] = useState<CourseProblem[]>([])
  const [catalog, setCatalog] = useState<ProgrammingProblemSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [busyId, setBusyId] = useState('')
  const [error, setError] = useState('')

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [current, all] = await Promise.all([getTeacherCourseProblems(course.id), getProgrammingProblems()])
      setAssigned(current)
      setCatalog(all)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Không thể tải bài tập của lớp.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void load() }, [course.id])

  const available = useMemo(() => {
    const assignedIds = new Set(assigned.map((problem) => problem.id))
    const keyword = query.trim().toLocaleLowerCase('vi')
    return catalog.filter((problem) => !assignedIds.has(problem.id)
      && (!keyword || `${problem.title} ${problem.summary}`.toLocaleLowerCase('vi').includes(keyword)))
  }, [assigned, catalog, query])

  async function assign(problem: ProgrammingProblemSummary) {
    setBusyId(problem.id)
    try {
      setAssigned(await assignTeacherCourseProblem(course.id, problem.id))
      onToast('Đã thêm bài tập vào lớp')
    } catch (reason) {
      onToast(reason instanceof Error ? reason.message : 'Không thể thêm bài tập.')
    } finally {
      setBusyId('')
    }
  }

  async function remove(problem: CourseProblem) {
    if (!window.confirm(`Gỡ bài “${problem.title}” khỏi lớp?`)) return
    setBusyId(problem.id)
    try {
      setAssigned(await removeTeacherCourseProblem(course.id, problem.id))
      onToast('Đã gỡ bài tập khỏi lớp')
    } catch (reason) {
      onToast(reason instanceof Error ? reason.message : 'Không thể gỡ bài tập.')
    } finally {
      setBusyId('')
    }
  }

  if (loading) return <div className="mt-6 space-y-3">{[1, 2, 3].map((item) => <div key={item} className="ui-skeleton h-20 rounded-xl" />)}</div>
  if (error) return <div className="mt-6 rounded-xl border border-red-200 bg-red-50 p-6 text-center text-sm font-semibold text-red-700">{error}<button type="button" onClick={() => void load()} className="ui-button-danger mx-auto mt-4 flex">Thử lại</button></div>

  return (
    <section className="mt-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-xl font-bold text-slate-950">Bài tập lập trình</h2>
          <p className="mt-1 text-sm text-slate-500">{assigned.length} bài đang được giao cho lớp.</p>
        </div>
        <button type="button" onClick={() => setModalOpen(true)} className="ui-button-primary">+ Thêm bài tập</button>
      </div>

      {assigned.length === 0 ? (
        <div className="ui-state mt-4">
          <div><h3 className="text-lg font-bold text-slate-900">Chưa có bài tập</h3><p className="mt-2 text-sm text-slate-500">Thêm bài lập trình để sinh viên bắt đầu làm bài.</p></div>
        </div>
      ) : (
        <div className="mt-4 space-y-3">
          {assigned.map((problem, index) => (
            <article key={problem.id} className="ui-card flex flex-col gap-4 p-4 sm:flex-row sm:items-center">
              <span className="grid h-10 w-10 shrink-0 place-items-center rounded-lg bg-blue-50 text-sm font-black text-blue-700">{index + 1}</span>
              <div className="min-w-0 flex-1">
                <h3 className="font-bold text-slate-950">{problem.title}</h3>
                <p className="mt-1 line-clamp-1 text-sm text-slate-500">{problem.summary}</p>
                <div className="mt-2 flex gap-2 text-xs"><span className="ui-badge">{topicLabels[problem.topic]}</span><span className="rounded-full bg-slate-100 px-2 py-1 font-bold text-slate-600">{difficultyLabels[problem.difficulty]}</span></div>
              </div>
              <div className="flex gap-2">
                <a href={`/problems/${encodeURIComponent(problem.slug)}`} className="ui-button-secondary flex-1">Xem bài</a>
                <button type="button" disabled={busyId === problem.id} onClick={() => void remove(problem)} className="ui-button-danger flex-1">Gỡ</button>
              </div>
            </article>
          ))}
        </div>
      )}

      {modalOpen ? (
        <ModalDialog title="Thêm bài tập vào lớp" onClose={() => setModalOpen(false)} maxWidth="max-w-2xl">
          <div className="p-5 sm:p-6">
            <p className="mb-4 font-mono text-xs font-bold text-blue-700">{course.code}</p>
            <input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm kiếm bài tập..." className="ui-control" />
            <div className="mt-4 max-h-96 space-y-2 overflow-y-auto">
              {available.length === 0 ? <p className="p-8 text-center text-sm text-slate-500">Không còn bài tập phù hợp để thêm.</p> : available.map((problem) => (
                <div key={problem.id} className="flex items-center gap-3 rounded-xl border border-slate-200 p-3 transition hover:border-blue-200 hover:bg-blue-50/40">
                  <div className="min-w-0 flex-1"><b className="block truncate text-sm">{problem.title}</b><span className="text-xs text-slate-500">{topicLabels[problem.topic]} · {difficultyLabels[problem.difficulty]}</span></div>
                  <button type="button" disabled={busyId === problem.id} onClick={() => void assign(problem)} className="ui-button-primary min-h-9 px-3 py-2">Thêm</button>
                </div>
              ))}
            </div>
          </div>
        </ModalDialog>
      ) : null}
    </section>
  )
}
