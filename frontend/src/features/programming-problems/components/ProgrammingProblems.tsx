import { useEffect, useState, type MouseEvent } from 'react'
import { getProgrammingProblems, getSolvedProgrammingProblemIds } from '../api/programmingProblemsApi'
import { ProblemWorkspace } from './ProblemWorkspace'
import {
  topicLabels,
  type ProblemDifficulty,
  type ProblemTopic,
  type ProgrammingProblemSummary,
  type SubmissionLanguage,
} from '../types/programmingProblem'
import { IconCheck, IconChevronDown, IconCode } from '../../../shared/components/Icons'

const topics = Object.entries(topicLabels) as [ProblemTopic, string][]
const difficultyLabels: Record<ProblemDifficulty, string> = {
  EASY: 'Dễ',
  MEDIUM: 'Trung bình',
  HARD: 'Khó',
}
const languageLabels: Record<SubmissionLanguage, string> = {
  CPP: 'C++', JAVA: 'Java', PYTHON: 'Python', HTML: 'HTML', MYSQL: 'MySQL',
}
type ProgressFilter = '' | 'SOLVED' | 'UNSOLVED'

interface ProgrammingProblemsProps {
  slug?: string
}

export function ProgrammingProblems({ slug }: ProgrammingProblemsProps) {
  const [selectedTopic, setSelectedTopic] = useState<ProblemTopic | null>(null)
  const [difficulty, setDifficulty] = useState<ProblemDifficulty | ''>('')
  const [language, setLanguage] = useState<SubmissionLanguage | ''>('')
  const [progress, setProgress] = useState<ProgressFilter>('')
  const [problems, setProblems] = useState<ProgrammingProblemSummary[]>([])
  const [solvedProblemIds, setSolvedProblemIds] = useState<Set<string>>(new Set())
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [flashMessage, setFlashMessage] = useState('')

  useEffect(() => {
    let ignore = false
    setLoading(true)
    setError('')
    void getProgrammingProblems({
      topic: selectedTopic ?? undefined,
      difficulty: difficulty || undefined,
      language: language || undefined,
    })
      .then((result) => { if (!ignore) setProblems(result) })
      .catch((reason: unknown) => {
        if (!ignore) setError(reason instanceof Error ? reason.message : 'Không thể tải bài tập.')
      })
      .finally(() => { if (!ignore) setLoading(false) })
    return () => { ignore = true }
  }, [selectedTopic, difficulty, language])

  useEffect(() => {
    void getSolvedProgrammingProblemIds()
      .then((problemIds) => setSolvedProblemIds(new Set(problemIds)))
      .catch(() => undefined)
  }, [])

  useEffect(() => {
    if (!flashMessage) return
    const timeoutId = window.setTimeout(() => setFlashMessage(''), 3300)
    return () => window.clearTimeout(timeoutId)
  }, [flashMessage])

  if (slug) return (
    <ProblemWorkspace
      slug={slug}
      onBack={returnToProblemList}
      onAccepted={(problemId) => {
        setSolvedProblemIds((current) => new Set(current).add(problemId))
        setFlashMessage('Đã lưu bài thành công')
        returnToProblemList()
      }}
    />
  )

  const visibleProblems = problems.filter((problem) => {
    if (progress === 'SOLVED') return solvedProblemIds.has(problem.id)
    if (progress === 'UNSOLVED') return !solvedProblemIds.has(problem.id)
    return true
  })

  return (
    <section className="mx-auto w-full lg:w-4/5 xl:w-3/5">
      {/* Toast Notification */}
      {flashMessage ? (
        <div role="status" className="flash-toast fixed right-4 top-20 z-50 flex max-w-[calc(100vw-2rem)] items-center gap-3 rounded-xl border border-blue-700 bg-blue-600 px-4 py-2.5 text-xs font-bold text-white shadow-lg shadow-blue-900/20 sm:right-6 lg:right-10">
          <span className="grid h-6 w-6 shrink-0 place-items-center rounded-full bg-white text-blue-600 shadow-sm" aria-hidden="true">
            <IconCheck className="h-4 w-4" />
          </span>
          <span>{flashMessage}</span>
        </div>
      ) : null}

      {/* Header Section */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <div className="inline-flex items-center gap-2 rounded-xl border border-blue-200 bg-blue-50 px-3 py-1 font-mono text-[11px] font-bold uppercase tracking-wider text-blue-700">
            <IconCode className="h-3.5 w-3.5" />
            <span>Practice library</span>
          </div>
          <h2 className="mt-3 text-2xl font-black tracking-tight text-slate-900 sm:text-3xl">Bài tập lập trình</h2>
          <p className="mt-1.5 text-xs leading-relaxed text-slate-600">
            Rèn luyện tư duy thuật toán và kỹ năng code qua các bài tập có hệ thống chấm tự động.
          </p>
        </div>
        <div className="flex items-center gap-2 rounded-xl border border-blue-100 bg-white px-3 py-1.5 text-xs font-semibold text-slate-600 shadow-sm">
          <span className="h-2 w-2 rounded-full bg-blue-500" />
          <span>{loading ? 'Đang tải...' : `${visibleProblems.length} bài tập`}</span>
        </div>
      </div>

      {/* Filter Toolbar Card */}
      <div className="relative z-20 mt-6 rounded-3xl border border-blue-100 bg-white p-4 shadow-md shadow-blue-900/10">
        <div className="flex flex-wrap gap-1.5">
          <button type="button" onClick={() => setSelectedTopic(null)} className={filterButtonClass(selectedTopic === null)}>Tất cả</button>
          {topics.map(([topic, label]) => (
            <button key={topic} type="button" onClick={() => setSelectedTopic(topic)} className={filterButtonClass(selectedTopic === topic)}>{label}</button>
          ))}
        </div>
        <div className="mt-3.5 flex flex-col gap-2.5 border-t border-blue-100 pt-3.5 sm:flex-row sm:flex-wrap sm:items-start sm:justify-end">
          <ProblemFilterDropdown
            label="Độ khó"
            value={difficulty}
            options={[
              { value: '', label: 'Tất cả độ khó' },
              ...Object.entries(difficultyLabels).map(([value, label]) => ({ value, label })),
            ]}
            onChange={(value) => setDifficulty(value as ProblemDifficulty | '')}
          />
          <ProblemFilterDropdown
            label="Ngôn ngữ"
            value={language}
            options={[
              { value: '', label: 'Tất cả ngôn ngữ' },
              ...Object.entries(languageLabels).map(([value, label]) => ({ value, label })),
            ]}
            onChange={(value) => setLanguage(value as SubmissionLanguage | '')}
          />
          <ProblemFilterDropdown
            label="Tiến độ"
            value={progress}
            options={[
              { value: '', label: 'Tất cả bài tập' },
              { value: 'SOLVED', label: 'Đã giải' },
              { value: 'UNSOLVED', label: 'Chưa giải' },
            ]}
            onChange={(value) => setProgress(value as ProgressFilter)}
          />
        </div>
      </div>

      {error ? <div className="mt-6 rounded-2xl border border-rose-500/30 bg-rose-500/10 p-5 text-xs font-bold text-rose-300">{error}</div> : null}
      {!error && loading ? (
        <div className="mt-5 space-y-3">
          {Array.from({ length: 5 }, (_, index) => (
            <div key={index} className="h-28 animate-pulse rounded-2xl border border-blue-100 bg-blue-50" />
          ))}
        </div>
      ) : null}
      {!error && !loading && visibleProblems.length === 0 ? (
        <div className="mt-5 rounded-3xl border border-dashed border-blue-200 bg-white p-12 text-center text-xs font-medium text-slate-600">
          Không có bài tập phù hợp với bộ lọc đã chọn.
        </div>
      ) : null}
      {!error && !loading ? <ProblemList problems={visibleProblems} solvedProblemIds={solvedProblemIds} onSelect={openProblem} /> : null}
    </section>
  )
}

function ProblemFilterDropdown({ label, value, options, onChange }: {
  label: string
  value: string
  options: { value: string; label: string }[]
  onChange: (value: string) => void
}) {
  const selectedLabel = options.find((option) => option.value === value)?.label ?? options[0].label

  function selectOption(event: MouseEvent<HTMLButtonElement>, nextValue: string) {
    onChange(nextValue)
    event.currentTarget.closest('details')?.removeAttribute('open')
  }

  return (
    <details className="problem-filter-dropdown" name="problem-filters">
      <summary className="problem-filter-trigger">
        <span className="problem-filter-label">{label}</span>
        <span className="problem-filter-value">{selectedLabel}</span>
        <span className="problem-filter-chevron" aria-hidden="true">
          <IconChevronDown className="h-3 w-3" />
        </span>
      </summary>
      <div className="problem-filter-menu" role="listbox" aria-label={label}>
        {options.map((option) => (
          <button
            key={option.value}
            type="button"
            role="option"
            aria-selected={option.value === value}
            className="problem-filter-option"
            onClick={(event) => selectOption(event, option.value)}
          >
            <span>{option.label}</span>
            {option.value === value ? <span className="font-bold text-blue-600" aria-hidden="true">✓</span> : null}
          </button>
        ))}
      </div>
    </details>
  )
}

function openProblem(slug: string) {
  window.history.pushState({ deveduProblemListReturn: true }, '', `/problems/${encodeURIComponent(slug)}`)
  window.dispatchEvent(new PopStateEvent('popstate'))
}

function returnToProblemList() {
  if (window.history.state?.deveduProblemListReturn === true) {
    window.history.back()
    return
  }
  window.history.replaceState(null, '', '/problems')
  window.dispatchEvent(new PopStateEvent('popstate'))
}

function ProblemList({ problems, solvedProblemIds, onSelect }: {
  problems: ProgrammingProblemSummary[]
  solvedProblemIds: Set<string>
  onSelect: (slug: string) => void
}) {
  return (
    <div className="mt-5 space-y-3">
      {problems.map((problem, index) => {
        const solved = solvedProblemIds.has(problem.id)
        return (
          <button
            key={problem.id}
            type="button"
            onClick={() => onSelect(problem.slug)}
            className="group flex w-full items-start gap-4 rounded-2xl border border-blue-100 bg-white p-5 text-left shadow-sm transition-all hover:border-blue-300 hover:shadow-md hover:shadow-blue-500/10 focus:outline-none focus:ring-2 focus:ring-blue-500/30"
          >
            {/* Solved Status Indicator */}
            <span
              className={`mt-0.5 grid h-9 w-9 shrink-0 place-items-center rounded-xl border text-xs font-black transition-all ${
                solved
                  ? 'border-emerald-500/50 bg-emerald-500 text-white shadow-md shadow-emerald-500/30 ring-2 ring-emerald-400/20'
                  : 'border-blue-100 bg-blue-50 text-transparent group-hover:border-blue-300 group-hover:text-blue-600'
              }`}
              aria-label={solved ? 'Đã hoàn thành' : 'Chưa hoàn thành'}
            >
              <IconCheck className="h-4 w-4" />
            </span>

            {/* Problem Details */}
            <span className="min-w-0 flex-1">
              <span className="flex flex-wrap items-center gap-2">
                <span className="rounded-lg border border-blue-500/30 bg-blue-500/10 px-2.5 py-0.5 font-mono text-[10px] font-bold text-blue-400">
                  {topicLabels[problem.topic]}
                </span>
                <span className={`rounded-lg px-2.5 py-0.5 text-[10px] font-bold ${difficultyClass(problem.difficulty)}`}>
                  {difficultyLabels[problem.difficulty]}
                </span>
                {problem.allowedLanguages.map((item) => (
                  <span key={item} className="rounded-lg border border-blue-100 bg-blue-50 px-2 py-0.5 font-mono text-[10px] font-semibold text-blue-700">
                    {languageLabels[item]}
                  </span>
                ))}
              </span>
              <span className="mt-2.5 block text-base font-bold text-slate-900 transition-colors group-hover:text-blue-700">
                {problem.title}
              </span>
              <span className="mt-1 block text-xs leading-5 text-slate-600 line-clamp-2">
                {problem.summary}
              </span>
            </span>

            {/* Problem Index */}
            <span className="shrink-0 rounded-lg border border-blue-100 bg-blue-50 px-2.5 py-1 font-mono text-xs font-bold text-blue-600 group-hover:border-blue-300 group-hover:text-blue-700">
              #{String(index + 1).padStart(2, '0')}
            </span>
          </button>
        )
      })}
    </div>
  )
}

function filterButtonClass(active: boolean) {
  return `rounded-xl px-3.5 py-1.5 text-xs font-bold transition-all ${
    active
      ? 'bg-blue-600 text-white shadow-sm shadow-blue-600/20'
      : 'border border-blue-100 bg-white text-slate-600 hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700'
  }`
}

function difficultyClass(difficulty: ProblemDifficulty) {
  if (difficulty === 'EASY') return 'border border-emerald-500/30 bg-emerald-500/10 text-emerald-400'
  if (difficulty === 'MEDIUM') return 'border border-amber-500/30 bg-amber-500/10 text-amber-400'
  return 'border border-rose-500/30 bg-rose-500/10 text-rose-400'
}
