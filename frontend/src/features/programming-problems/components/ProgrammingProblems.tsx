import { useEffect, useState } from 'react'
import { getProgrammingProblems } from '../api/programmingProblemsApi'
import { ProblemWorkspace } from './ProblemWorkspace'
import {
  topicLabels,
  type ProblemTopic,
  type ProgrammingProblemSummary,
} from '../types/programmingProblem'

const topics = Object.entries(topicLabels) as [ProblemTopic, string][]

export function ProgrammingProblems() {
  const [selectedTopic, setSelectedTopic] = useState<ProblemTopic | 'ALL'>('ALL')
  const [selectedSlug, setSelectedSlug] = useState<string | null>(null)
  const [problems, setProblems] = useState<ProgrammingProblemSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false
    setLoading(true)
    setError('')

    void getProgrammingProblems(selectedTopic === 'ALL' ? undefined : selectedTopic)
      .then((result) => {
        if (!ignore) setProblems(result)
      })
      .catch((reason: unknown) => {
        if (!ignore) {
          setProblems([])
          setError(reason instanceof Error ? reason.message : 'Không thể tải bài tập.')
        }
      })
      .finally(() => {
        if (!ignore) setLoading(false)
      })

    return () => {
      ignore = true
    }
  }, [selectedTopic])

  if (selectedSlug) {
    return <ProblemWorkspace slug={selectedSlug} onBack={() => setSelectedSlug(null)} />
  }

  return (
    <section>
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="font-mono text-xs uppercase tracking-[0.2em] text-cyan-400">Practice library</p>
          <h2 className="mt-2 text-2xl font-bold tracking-tight text-white sm:text-3xl">
            Programming Problems
          </h2>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-400">
            Chọn một chủ đề, đọc đề và bắt đầu viết lời giải theo ngôn ngữ bạn muốn.
          </p>
        </div>
        <span className="text-sm text-slate-500">
          {loading ? 'Đang tải...' : `${problems.length} bài tập`}
        </span>
      </div>

      <div className="mt-6 flex gap-2 overflow-x-auto pb-2" aria-label="Lọc bài tập theo chủ đề">
        <button
          type="button"
          onClick={() => setSelectedTopic('ALL')}
          className={`shrink-0 rounded-full border px-3 py-2 text-xs font-medium transition ${
            selectedTopic === 'ALL'
              ? 'border-cyan-400 bg-cyan-400 text-slate-950'
              : 'border-slate-700 bg-slate-900 text-slate-400 hover:border-slate-500 hover:text-white'
          }`}
        >
          Tất cả
        </button>
        {topics.map(([value, label]) => (
          <button
            key={value}
            type="button"
            onClick={() => setSelectedTopic(value)}
            className={`shrink-0 rounded-full border px-3 py-2 text-xs font-medium transition ${
              selectedTopic === value
                ? 'border-cyan-400 bg-cyan-400 text-slate-950'
                : 'border-slate-700 bg-slate-900 text-slate-400 hover:border-slate-500 hover:text-white'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {error ? (
        <div className="mt-5 rounded-xl border border-rose-400/20 bg-rose-400/5 p-5 text-sm text-rose-200">
          {error}
        </div>
      ) : null}

      {!error && !loading ? (
        <div className="mt-5 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
          {problems.map((problem, index) => (
            <button
              key={problem.id}
              type="button"
              onClick={() => setSelectedSlug(problem.slug)}
              className="group flex min-h-44 flex-col rounded-xl border border-slate-800 bg-slate-900/60 p-5 text-left transition hover:-translate-y-0.5 hover:border-cyan-400/40 hover:bg-slate-900 focus:outline-none focus:ring-2 focus:ring-cyan-400/40"
            >
              <div className="flex w-full items-center justify-between gap-3">
                <span className="rounded-full border border-slate-700 px-2.5 py-1 text-[11px] font-medium text-slate-400 group-hover:border-cyan-400/20 group-hover:text-cyan-300">
                  {topicLabels[problem.topic]}
                </span>
                <span className="font-mono text-xs text-slate-700">
                  {String(index + 1).padStart(2, '0')}
                </span>
              </div>
              <h3 className="mt-4 font-semibold text-slate-100 group-hover:text-cyan-200">
                {problem.title}
              </h3>
              <p className="mt-2 line-clamp-2 text-sm leading-6 text-slate-500">{problem.summary}</p>
              <span className="mt-auto pt-4 text-xs font-semibold text-cyan-400/80">
                Mở bài tập <span aria-hidden="true">→</span>
              </span>
            </button>
          ))}
        </div>
      ) : null}

      {!error && loading ? (
        <div className="mt-5 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 6 }, (_, index) => (
            <div key={index} className="h-44 animate-pulse rounded-xl border border-slate-800 bg-slate-900/50" />
          ))}
        </div>
      ) : null}
    </section>
  )
}

