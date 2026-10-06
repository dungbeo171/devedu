import { useEffect, useRef, useState } from 'react'
import { deleteProgrammingProblem, getProgrammingProblems, getSolvedProgrammingProblemIds } from '../api/programmingProblemsApi'
import { ProblemWorkspace } from './ProblemWorkspace'
import { getStoredUser } from '../../auth/api/authApi'
import {
  topicLabels,
  type ProblemDifficulty,
  type ProblemTopic,
  type ProgrammingProblemSummary,
  type SubmissionLanguage,
} from '../types/programmingProblem'
import { IconCheck, IconChevronDown, IconCode, IconFilter, IconSearch } from '../../../shared/components/Icons'
import { problemListUrl, readProblemListState, type ProblemListState, type ProgressFilter } from '../problemListState'

const topics = Object.entries(topicLabels) as [ProblemTopic, string][]
const difficultyLabels: Record<ProblemDifficulty, string> = {
  EASY: 'Dễ',
  MEDIUM: 'Trung bình',
  HARD: 'Khó',
}
const languageLabels: Record<SubmissionLanguage, string> = {
  CPP: 'C++', JAVA: 'Java', PYTHON: 'Python', HTML: 'HTML', MYSQL: 'MySQL',
}
const problemsPerPage = 20

interface ProgrammingProblemsProps {
  slug?: string
}

export function ProgrammingProblems({ slug }: ProgrammingProblemsProps) {
  const [filters, setFilters] = useState(() => readProblemListState(window.location.search))
  const { topic: selectedTopic, difficulty, language, progress, search, page } = filters
  const filterPanelRef = useRef<HTMLDetailsElement>(null)
  const [closeFilterAfterLoad, setCloseFilterAfterLoad] = useState(false)
  const [loadedFilterKey, setLoadedFilterKey] = useState('')
  const filterKey = JSON.stringify([selectedTopic, difficulty, language])
  const [problems, setProblems] = useState<ProgrammingProblemSummary[]>([])
  const [solvedProblemIds, setSolvedProblemIds] = useState<Set<string>>(new Set())
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [flashMessage, setFlashMessage] = useState('')
  const currentUser = getStoredUser()
  const [progressLoading, setProgressLoading] = useState(Boolean(currentUser))

  function changeFilters(patch: Partial<ProblemListState>, closeWhenReady = true) {
    setFilters(current => ({ ...current, ...patch, page: 1 }))
    setCloseFilterAfterLoad(closeWhenReady)
  }

  useEffect(() => {
    if (!slug) window.history.replaceState(window.history.state, '', problemListUrl(filters))
  }, [filters, slug])

  useEffect(() => {
    if (!closeFilterAfterLoad || loading || progressLoading || error || loadedFilterKey !== filterKey) return
    const panel = filterPanelRef.current
    if (panel?.open) {
      const focusWasInside = panel.contains(document.activeElement)
      panel.open = false
      if (focusWasInside) panel.querySelector('summary')?.focus({ preventScroll: true })
    }
    setCloseFilterAfterLoad(false)
  }, [closeFilterAfterLoad, loading, progressLoading, error, loadedFilterKey, filterKey, filters])

  async function deleteProblem(problem: ProgrammingProblemSummary) {
    if (!window.confirm(`Xóa bài tập “${problem.title}”? Hành động này không thể hoàn tác.`)) return
    setError('')
    try {
      await deleteProgrammingProblem(problem.slug)
      setProblems((current) => current.filter((item) => item.id !== problem.id))
      setSolvedProblemIds((current) => {
        const next = new Set(current)
        next.delete(problem.id)
        return next
      })
      setFlashMessage(`Đã xóa bài tập “${problem.title}”`)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Không thể xóa bài tập.')
    }
  }

  useEffect(() => {
    let ignore = false
    setLoading(true)
    setError('')
    void getProgrammingProblems({
      topic: selectedTopic ?? undefined,
      difficulty: difficulty || undefined,
      language: language || undefined,
    })
      .then((result) => {
        if (!ignore) { setProblems(result); setLoadedFilterKey(filterKey) }
      })
      .catch((reason: unknown) => {
        if (!ignore) setError(reason instanceof Error ? reason.message : 'Không thể tải bài tập.')
      })
      .finally(() => { if (!ignore) setLoading(false) })
    return () => { ignore = true }
  }, [selectedTopic, difficulty, language])

  useEffect(() => {
    let ignore = false
    if (!currentUser) {
      setSolvedProblemIds(new Set())
      setProgressLoading(false)
      return
    }
    setProgressLoading(true)
    void getSolvedProgrammingProblemIds()
      .then((problemIds) => { if (!ignore) setSolvedProblemIds(new Set(problemIds)) })
      .catch(() => undefined)
      .finally(() => { if (!ignore) setProgressLoading(false) })
    return () => { ignore = true }
  }, [currentUser?.id, currentUser?.role])

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

  const keyword = search.trim().toLocaleLowerCase('vi')
  const visibleProblems = problems
    .filter((problem) => {
      if (progress === 'SOLVED' && !solvedProblemIds.has(problem.id)) return false
      if (progress === 'UNSOLVED' && solvedProblemIds.has(problem.id)) return false
      if (!keyword) return true
      return `${problem.title} ${problem.summary} ${topicLabels[problem.topic]}`.toLocaleLowerCase('vi').includes(keyword)
    })
  const activeFilterCount = Number(Boolean(search.trim())) + Number(Boolean(difficulty)) + Number(Boolean(language)) + Number(Boolean(progress))
  const totalPages = Math.max(1, Math.ceil(visibleProblems.length / problemsPerPage))
  const currentPage = Math.min(page, totalPages)
  const pageStart = (currentPage - 1) * problemsPerPage
  const pageProblems = visibleProblems.slice(pageStart, pageStart + problemsPerPage)

  return (
    <section className="mx-auto w-full">
      {/* Toast Notification */}
      {flashMessage ? (
        <div role="status" className="flash-toast fixed right-4 top-20 z-50 flex max-w-[calc(100vw-2rem)] items-center gap-3 rounded-xl border border-blue-700 bg-blue-600 px-4 py-2.5 text-xs font-bold text-white shadow-lg shadow-blue-900/20 sm:right-6 lg:right-10">
          <span className="grid h-6 w-6 shrink-0 place-items-center rounded-full bg-white text-blue-600 shadow-sm" aria-hidden="true">
            <IconCheck className="h-4 w-4" />
          </span>
          <span>{flashMessage}</span>
        </div>
      ) : null}

      <div className="ui-page-header">
        <div>
          <h1 className="ui-page-title">Bài tập lập trình</h1>

        </div>
        <div className="ui-badge">
          <span className="h-2 w-2 rounded-full bg-blue-500" />
          <span>{loading ? 'Đang tải...' : `${visibleProblems.length} bài tập`}</span>
        </div>
      </div>

      {currentUser?.role === 'TEACHER' || currentUser?.role === 'ADMIN' ? (
        <a href="/problems/add" className="ui-button-primary mt-5">
          <IconCode className="h-4 w-4" />
          <span>Thêm bài tập</span>
        </a>
      ) : null}

      <div className="mt-5 flex gap-1.5 overflow-x-auto pb-1">
        <button type="button" onClick={() => changeFilters({ topic: null })} className={`${filterButtonClass(selectedTopic === null)} shrink-0`}>Tất cả</button>
        {topics.map(([topic, label]) => (
          <button key={topic} type="button" onClick={() => changeFilters({ topic })} className={`${filterButtonClass(selectedTopic === topic)} shrink-0`}>{label}</button>
        ))}
      </div>

      <div className="relative z-40 mt-3 flex flex-wrap items-center gap-2 overflow-visible">
        <details ref={filterPanelRef} className="group relative open:z-[90]" name="problem-filter-panel">
          <summary className={`relative grid h-9 w-9 list-none place-items-center rounded-full transition [&::-webkit-details-marker]:hidden ${activeFilterCount ? 'bg-blue-50 text-blue-600 ring-1 ring-blue-200' : 'bg-slate-100 text-slate-500 hover:bg-slate-200'}`} aria-label="Mở bộ lọc">
            <IconFilter className="h-4 w-4" />
            {activeFilterCount ? <span className="absolute right-0 top-0 grid h-4 min-w-4 place-items-center rounded-full bg-blue-600 px-1 text-[9px] font-bold text-white">{activeFilterCount}</span> : null}
          </summary>
          <div className="absolute left-0 top-[calc(100%+.5rem)] z-[100] max-h-[min(26rem,calc(100vh-8rem))] w-[min(34rem,calc(100vw-2rem))] overflow-y-auto rounded-xl border border-slate-200 bg-white p-4 shadow-[0_18px_45px_-18px_rgba(15,23,42,.35)]">
            <label className="block text-xs font-semibold text-slate-600">
              Tìm kiếm bài tập
              <span className="relative mt-1.5 block">
                <IconSearch className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <input
                  type="search"
                  value={search}
                  onChange={(event) => changeFilters({ search: event.target.value }, false)}
                  onKeyDown={(event) => { if (event.key === 'Enter') { event.preventDefault(); setCloseFilterAfterLoad(true) } }}
                  placeholder="Nhập tên bài tập, nhấn Enter..."
                  className="ui-control ui-control-with-leading-icon pr-3 text-sm font-medium"
                />
              </span>
            </label>
            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              <ProblemFilterSelect label="Độ khó" value={difficulty} options={[{ value: '', label: 'Tất cả độ khó' }, ...Object.entries(difficultyLabels).map(([value, label]) => ({ value, label }))]} onChange={(value) => changeFilters({ difficulty: value as ProblemDifficulty | '' })} />
              <ProblemFilterSelect label="Ngôn ngữ" value={language} options={[{ value: '', label: 'Tất cả ngôn ngữ' }, ...Object.entries(languageLabels).map(([value, label]) => ({ value, label }))]} onChange={(value) => changeFilters({ language: value as SubmissionLanguage | '' })} />
              {currentUser ? <ProblemFilterSelect label="Tiến độ" value={progress} options={[{ value: '', label: 'Tất cả bài tập' }, { value: 'SOLVED', label: 'Đã giải' }, { value: 'UNSOLVED', label: 'Chưa giải' }]} onChange={(value) => changeFilters({ progress: value as ProgressFilter })} /> : null}
            </div>
            {activeFilterCount > 0 ? (
              <button
                type="button"
                onClick={() => changeFilters({ search: '', difficulty: '', language: '', progress: '' })}
                className="mt-3 text-xs font-semibold text-blue-700 hover:text-blue-800 hover:underline"
              >
                Xóa bộ lọc
              </button>
            ) : null}
            {closeFilterAfterLoad && (loading || progressLoading || loadedFilterKey !== filterKey) ? <p role="status" className="mt-3 text-xs text-slate-500">Đang lọc bài tập…</p> : null}
          </div>
        </details>
        <div className="ml-auto flex items-center gap-2 pl-2 text-xs text-slate-500">
          {currentUser ? <span className="h-4 w-4 rounded-full border-2 border-slate-200 border-t-emerald-500" aria-hidden="true" /> : null}
          <span>{currentUser ? `${solvedProblemIds.size}/${problems.length} đã giải` : `${visibleProblems.length} bài tập`}</span>
        </div>
      </div>

      {error ? <div role="alert" className="mt-6 rounded-xl border border-red-200 bg-red-50 p-5 text-sm font-semibold text-red-700">{error}</div> : null}
      {!error && loading ? (
        <div className="mt-5 space-y-3">
          {Array.from({ length: 5 }, (_, index) => (
            <div key={index} className="ui-skeleton h-20 rounded-md border border-slate-200" />
          ))}
        </div>
      ) : null}
      {!error && !loading && visibleProblems.length === 0 ? (
        <div className="ui-state mt-5 text-sm font-medium">
          Không có bài tập phù hợp với bộ lọc đã chọn.
        </div>
      ) : null}
      {!error && !loading ? (
        <>
          <ProblemList
            problems={pageProblems}
            solvedProblemIds={solvedProblemIds}
            onSelect={openProblem}
            onDelete={(problem) => void deleteProblem(problem)}
            startIndex={pageStart}
            admin={currentUser?.role === 'ADMIN'}
          />
          {visibleProblems.length > problemsPerPage ? (
            <Pagination page={currentPage} totalPages={totalPages} onChange={page => setFilters(current => ({ ...current, page }))} />
          ) : null}
        </>
      ) : null}
    </section>
  )
}

function ProblemFilterSelect({ label, value, options, onChange }: {
  label: string
  value: string
  options: { value: string; label: string }[]
  onChange: (value: string) => void
}) {
  return (
    <label className="block min-w-0 text-xs font-semibold text-slate-600">
      {label}
      <span className="relative mt-1.5 block">
        <select
          value={value}
          onChange={(event) => onChange(event.target.value)}
          className="ui-control appearance-none pr-9 text-sm font-semibold"
        >
        {options.map((option) => (
          <option
            key={option.value}
            value={option.value}
          >
            {option.label}
          </option>
        ))}
        </select>
        <IconChevronDown className="pointer-events-none absolute right-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-blue-700" />
      </span>
    </label>
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

function ProblemList({ problems, solvedProblemIds, onSelect, onDelete, startIndex, admin }: {
  problems: ProgrammingProblemSummary[]
  solvedProblemIds: Set<string>
  onSelect: (slug: string) => void
  onDelete: (problem: ProgrammingProblemSummary) => void
  startIndex: number
  admin: boolean
}) {
  return (
    <div className="mt-4 overflow-hidden rounded-lg border border-slate-100">
      {problems.map((problem, index) => {
        const solved = solvedProblemIds.has(problem.id)
        return (
          <div
            key={problem.id}
            className="group flex w-full items-stretch overflow-hidden border-b border-white bg-slate-50/70 transition-colors odd:bg-white last:border-b-0 hover:bg-blue-50/40"
          >
            <button
              type="button"
              onClick={() => onSelect(problem.slug)}
              className="grid min-h-14 min-w-0 flex-1 cursor-pointer grid-cols-[28px_minmax(0,1fr)_auto] items-center gap-3 px-3 py-3 text-left focus:outline-none focus:ring-2 focus:ring-inset focus:ring-blue-500/20 sm:grid-cols-[28px_minmax(0,1fr)_110px_76px_120px] sm:px-4"
            >
              <span
                className={`grid h-7 w-7 shrink-0 place-items-center text-sm font-semibold transition-colors ${
                  solved
                    ? 'text-emerald-600'
                    : 'text-transparent group-hover:text-slate-300'
                }`}
                aria-label={solved ? 'Đã hoàn thành' : 'Chưa hoàn thành'}
              >
                <IconCheck className="h-4 w-4" />
              </span>

              <span className="min-w-0 truncate text-sm font-semibold text-slate-900 transition-colors group-hover:text-blue-700">
                <span className="mr-2 text-slate-400">{startIndex + index + 1}.</span>{problem.title}
              </span>
              <span className="hidden text-right text-xs text-slate-500 sm:block">
                {problem.acceptanceRate.toFixed(1)}%
              </span>
              <span className={`justify-self-end text-xs font-medium ${difficultyTextClass(problem.difficulty)}`}>
                {difficultyLabels[problem.difficulty]}
              </span>
              <span className="hidden truncate text-right font-mono text-[11px] text-slate-500 sm:block">
                {problem.allowedLanguages.map((item) => languageLabels[item]).join(', ')}
              </span>
            </button>
            {admin ? (
              <div className="flex shrink-0 flex-col justify-center gap-1.5 border-l border-slate-200 bg-slate-50/50 px-3">
                <a href={`/problems/${encodeURIComponent(problem.slug)}/edit`} className="cursor-pointer rounded-md bg-blue-600 px-3 py-1.5 text-center text-[11px] font-semibold text-white transition hover:bg-blue-700">Sửa</a>
                <button type="button" onClick={() => onDelete(problem)} className="cursor-pointer rounded-md border border-red-200 bg-white px-3 py-1.5 text-[11px] font-semibold text-red-600 transition hover:bg-red-50">Xóa</button>
              </div>
            ) : null}
          </div>
        )
      })}
    </div>
  )
}

function Pagination({ page, totalPages, onChange }: {
  page: number
  totalPages: number
  onChange: (page: number) => void
}) {
  return (
    <nav className="mt-6 flex flex-wrap items-center justify-center gap-2" aria-label="Phân trang bài tập">
      <button
        type="button"
        disabled={page === 1}
        onClick={() => onChange(page - 1)}
        className="rounded-md border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-600 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
      >
        Trước
      </button>
      {Array.from({ length: totalPages }, (_, index) => index + 1).map((pageNumber) => (
        <button
          key={pageNumber}
          type="button"
          aria-current={pageNumber === page ? 'page' : undefined}
          onClick={() => onChange(pageNumber)}
          className={`grid h-9 min-w-9 place-items-center rounded-md px-2 text-xs font-semibold transition ${pageNumber === page ? 'bg-blue-600 text-white' : 'border border-slate-200 bg-white text-slate-600 hover:bg-slate-50'}`}
        >
          {pageNumber}
        </button>
      ))}
      <button
        type="button"
        disabled={page === totalPages}
        onClick={() => onChange(page + 1)}
        className="rounded-md border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-600 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
      >
        Sau
      </button>
    </nav>
  )
}

function filterButtonClass(active: boolean) {
  return `rounded-md px-3 py-1.5 text-xs font-medium transition-colors ${
    active
      ? 'bg-slate-800 text-white'
      : 'bg-slate-100 text-slate-600 hover:bg-slate-200 hover:text-slate-900'
  }`
}

function difficultyTextClass(difficulty: ProblemDifficulty) {
  if (difficulty === 'EASY') return 'text-emerald-600'
  if (difficulty === 'MEDIUM') return 'text-amber-600'
  return 'text-red-600'
}
