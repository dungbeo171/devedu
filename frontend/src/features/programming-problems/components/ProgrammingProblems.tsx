import { useEffect, useMemo, useState } from 'react'
import { getProgrammingProblems } from '../api/programmingProblemsApi'
import { ProblemWorkspace } from './ProblemWorkspace'
import { topicLabels, type ProblemTopic, type ProgrammingProblemSummary } from '../types/programmingProblem'

const topics = Object.entries(topicLabels) as [ProblemTopic, string][]

const topicDescriptions: Record<ProblemTopic, string> = {
  INTRODUCTION: 'Nhập xuất, điều kiện và vòng lặp cơ bản',
  CPP: 'Cú pháp, STL và tư duy giải bài với C++',
  JAVA: 'Mảng, chuỗi và nền tảng Java',
  PYTHON: 'Xử lý dữ liệu ngắn gọn với Python',
  OOP: 'Class, object và đóng gói dữ liệu',
  DATA_STRUCTURES: 'Stack, queue và các cấu trúc dữ liệu',
  ALGORITHMS: 'Tìm kiếm, sắp xếp và thuật toán nền tảng',
  SQL: 'Truy vấn, lọc và tổng hợp dữ liệu',
}

const topicMarks: Record<ProblemTopic, string> = {
  INTRODUCTION: '01', CPP: 'C++', JAVA: 'J', PYTHON: 'Py', OOP: '{}',
  DATA_STRUCTURES: '[]', ALGORITHMS: 'Fx', SQL: 'DB',
}

export function ProgrammingProblems() {
  const [selectedTopic, setSelectedTopic] = useState<ProblemTopic | null>(null)
  const [selectedSlug, setSelectedSlug] = useState<string | null>(null)
  const [problems, setProblems] = useState<ProgrammingProblemSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false
    void getProgrammingProblems()
      .then((result) => { if (!ignore) setProblems(result) })
      .catch((reason: unknown) => {
        if (!ignore) setError(reason instanceof Error ? reason.message : 'Không thể tải bài tập.')
      })
      .finally(() => { if (!ignore) setLoading(false) })
    return () => { ignore = true }
  }, [])

  const topicProblems = useMemo(
    () => selectedTopic ? problems.filter((problem) => problem.topic === selectedTopic) : [],
    [problems, selectedTopic],
  )

  if (selectedSlug) return <ProblemWorkspace slug={selectedSlug} onBack={() => setSelectedSlug(null)} />

  if (selectedTopic) {
    return (
      <section>
        <button type="button" onClick={() => setSelectedTopic(null)} className="inline-flex items-center gap-2 text-sm font-medium text-slate-600 transition hover:text-blue-600">
          <span aria-hidden="true">←</span> Tất cả chủ đề
        </button>
        <div className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="font-mono text-xs uppercase tracking-[0.2em] text-blue-600">{topicMarks[selectedTopic]} · Chủ đề</p>
            <h2 className="mt-2 text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">{topicLabels[selectedTopic]}</h2>
            <p className="mt-2 text-sm text-slate-600">{topicDescriptions[selectedTopic]}</p>
          </div>
          <span className="text-sm text-slate-500">{topicProblems.length} bài tập</span>
        </div>
        <ProblemList problems={topicProblems} onSelect={setSelectedSlug} />
      </section>
    )
  }

  return (
    <section>
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="font-mono text-xs uppercase tracking-[0.2em] text-blue-600">Practice library</p>
          <h2 className="mt-2 text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">Chọn chủ đề bài tập</h2>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600">Chọn chủ đề bạn muốn luyện tập, sau đó mở một bài để viết code và submit.</p>
        </div>
        <span className="text-sm text-slate-500">{loading ? 'Đang tải...' : `${problems.length} bài tập`}</span>
      </div>

      {error ? <div className="mt-6 rounded-xl border border-rose-400/20 bg-rose-400/5 p-5 text-sm text-rose-200">{error}</div> : null}
      {!error && !loading ? (
        <div className="mt-7 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {topics.map(([topic, label]) => {
            const count = problems.filter((problem) => problem.topic === topic).length
            return (
              <button key={topic} type="button" onClick={() => setSelectedTopic(topic)} className="group flex min-h-52 flex-col rounded-2xl border border-blue-100 bg-white/60 p-5 text-left transition hover:-translate-y-1 hover:border-blue-500/40 hover:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500/40">
                <span className="grid h-11 w-11 place-items-center rounded-xl border border-blue-500/20 bg-blue-600/10 font-mono text-sm font-black text-blue-600">{topicMarks[topic]}</span>
                <h3 className="mt-5 text-lg font-bold text-slate-900 group-hover:text-blue-700">{label}</h3>
                <p className="mt-2 text-sm leading-6 text-slate-500">{topicDescriptions[topic]}</p>
                <div className="mt-auto flex items-center justify-between pt-5 text-xs">
                  <span className="font-semibold text-slate-600">{count} bài tập</span>
                  <span className="font-bold text-blue-600">Mở <span aria-hidden="true">→</span></span>
                </div>
              </button>
            )
          })}
        </div>
      ) : null}
      {!error && loading ? <div className="mt-7 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{Array.from({ length: 8 }, (_, index) => <div key={index} className="h-52 animate-pulse rounded-2xl border border-blue-100 bg-white/50" />)}</div> : null}
    </section>
  )
}

function ProblemList({ problems, onSelect }: { problems: ProgrammingProblemSummary[]; onSelect: (slug: string) => void }) {
  return (
    <div className="mt-7 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
      {problems.map((problem, index) => (
        <button key={problem.id} type="button" onClick={() => onSelect(problem.slug)} className="group flex min-h-44 flex-col rounded-xl border border-blue-100 bg-white/60 p-5 text-left transition hover:-translate-y-0.5 hover:border-blue-500/40 hover:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500/40">
          <div className="flex w-full items-center justify-between gap-3">
            <span className="rounded-full border border-blue-200 px-2.5 py-1 text-[11px] font-medium text-slate-600 group-hover:border-blue-500/20 group-hover:text-blue-600">{topicLabels[problem.topic]}</span>
            <span className="font-mono text-xs text-slate-700">{String(index + 1).padStart(2, '0')}</span>
          </div>
          <h3 className="mt-4 font-semibold text-slate-900 group-hover:text-blue-700">{problem.title}</h3>
          <p className="mt-2 line-clamp-2 text-sm leading-6 text-slate-500">{problem.summary}</p>
          <span className="mt-auto pt-4 text-xs font-semibold text-blue-600/80">Mở bài tập <span aria-hidden="true">→</span></span>
        </button>
      ))}
    </div>
  )
}
