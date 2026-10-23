import { useCallback, useEffect, useRef, useState } from 'react'
import { getStoredUser } from '../../auth/api/authApi'
import { ProblemWorkspace } from '../../programming-problems/components/ProblemWorkspace'
import type { ProblemSubmission } from '../../programming-problems/types/programmingProblem'
import { FlashToast } from '../../../shared/components/FlashToast'
import { getContestProblem, registerContest, runContestProblem, startContestAttempt, submitContestProblem } from '../api/contestApi'
import type { ContestDetail, ContestSubmit, ContestResult } from '../types/contest'
import { useContestDrafts } from '../hooks/useContestDrafts'
import { ContestResultSummary } from './ContestResultSummary'
import { useContestResource } from '../hooks/useContestResource'
import { contestStatus, countdown, durationLabel } from '../contestTime'
import { verdictLabel, languageLabel, difficultyLabel } from '../contestPresentation'
import { ContestStatusBadge } from './ContestStatusBadge'
import { ContestEmpty, ContestError, ContestLoading } from './ContestStates'
import { ContestLeaderboard, ContestSubmissions } from './ContestResults'
import { ContestCameraPanel } from '../camera/ContestCameraPanel'

const CONTEST_PROBLEM_COPY_EVENT = 'devedu:contest-problem-copy'

type Tab = 'problems' | 'leaderboard' | 'submissions'
const tabs: { id: Tab; name: string }[] = [
  { id: 'problems', name: 'Bài tập' }, { id: 'leaderboard', name: 'Bảng xếp hạng' },
  { id: 'submissions', name: 'Bài nộp của tôi' },
]
function locationState() {
  const parts = window.location.pathname.split('/')
  const query = new URLSearchParams(window.location.search)
  const tab = tabs.find(t => t.id === parts[3])?.id ?? tabs.find(t => t.id === query.get('tab'))?.id ?? 'problems'
  return { tab, problem: parts[3] === 'problems' ? parts[4] ?? null : query.get('problem') }
}
function navigate(contestId: string, tab: Tab, problem: string | null = null, replace = false) {
  if (window.location.pathname.endsWith('/virtual')) {
    const params = new URLSearchParams(window.location.search)
    params.delete('enter')
    params.set('tab', tab)
    if (problem) params.set('problem', problem); else params.delete('problem')
    window.history[replace ? 'replaceState' : 'pushState'](null, '', window.location.pathname + '?' + params)
    window.dispatchEvent(new PopStateEvent('popstate'))
    return
  }
  const suffix = problem ? '/problems/' + encodeURIComponent(problem) : tab === 'problems' ? '' : '/' + tab
  window.history[replace ? 'replaceState' : 'pushState'](null, '', '/contests/' + encodeURIComponent(contestId) + suffix)
  window.dispatchEvent(new PopStateEvent('popstate'))
}
export function ContestDetailPage({ contestId }: { contestId: string }) {
  const resource = useContestResource<ContestResult>('/api/contests/' + encodeURIComponent(contestId) + '/results', 5000)
  return <ContestExperience contestId={contestId} resource={{ ...resource, data: resource.data?.detail ?? null }} />
}
export function ContestExperience({ contestId, resource, virtualId }: { contestId: string; resource: ReturnType<typeof useContestResource<ContestDetail>>; virtualId?: string }) {
  const { data, loading, error, now, reload, refreshing } = resource
  const [location, setLocation] = useState(locationState)
  const [registering, setRegistering] = useState(false)
  const [starting, setStarting] = useState(false)
  const [cameraReady, setCameraReady] = useState(false)
  const [actionError, setActionError] = useState('')
  const [toast, setToast] = useState('')
  const [pending, setPending] = useState<string | null>(null)
  const [lastSubmission, setLastSubmission] = useState<ProblemSubmission | null>(null)
  const [workspaceSideTab, setWorkspaceSideTab] = useState<'list' | 'statement' | null>('statement')
  const submissionInFlight = useRef(false)
  const user = getStoredUser()
  const userId = user?.publicId
  const { drafts, persist } = useContestDrafts(String(userId) + ':' + contestId + ':' + (virtualId ?? 'official'))
  const status = data ? contestStatus(data.contest, now) : null
  const selectedProblem = data?.problems.find(problem => problem.id === location.problem)
  const loadContestOwnedProblem = useCallback((_slug: string) => {
    if (!selectedProblem) return Promise.reject(new Error('Không tìm thấy bài tập trong kỳ thi.'))
    return getContestProblem(contestId, selectedProblem.id)
  }, [contestId, selectedProblem?.id])
  const runContestOwnedProblem = useCallback((_slug: string, language: Parameters<typeof runContestProblem>[2], sourceCode: string) => {
    if (!selectedProblem) return Promise.reject(new Error('Không tìm thấy bài tập trong kỳ thi.'))
    return runContestProblem(contestId, selectedProblem.id, language, sourceCode, virtualId)
  }, [contestId, selectedProblem?.id, virtualId])
  const transitioned = data && status !== data.contest.status
  useEffect(() => { if (transitioned) void reload() }, [transitioned, reload])
  useEffect(() => { setLastSubmission(null) }, [userId, virtualId])
  useEffect(() => {
    const change = () => setLocation(locationState())
    window.addEventListener('popstate', change)
    return () => window.removeEventListener('popstate', change)
  }, [])
  useEffect(() => {
    if (data && new URLSearchParams(window.location.search).has('enter')) {
      const first = data.problems.find(p => p.available)
      if (first) navigate(contestId, 'problems', first.id, true)
    }
  }, [data, contestId])
  async function register() {
    if (registering) return
    if (!user) { window.location.assign('/login'); return }
    setRegistering(true); setActionError('')
    try { await registerContest(contestId); await reload(); setToast('Đăng ký cuộc thi thành công') }
    catch (reason) { setActionError(reason instanceof Error ? reason.message : 'Không thể đăng ký.') }
    finally { setRegistering(false) }
  }
  async function beginAttempt() {
    if (starting) return
    if (data?.contest.monitoringEnabled && !cameraReady) { setActionError('Cho phép và kết nối camera trước khi bắt đầu làm bài.'); return }
    setStarting(true); setActionError('')
    try {
      await startContestAttempt(contestId)
      await reload()
      const first = data?.problems.find(p => p.available)
      if (first && !location.problem) navigate(contestId, 'problems', first.id)
    } catch (reason) { setActionError(reason instanceof Error ? reason.message : 'Không thể bắt đầu làm bài.') }
    finally { setStarting(false) }
  }
  const submit: ContestSubmit = async (slug, language, sourceCode) => {
    const problem = data?.problems.find(p => p.slug === slug)
    if (!problem || !data?.registered) throw new Error('Đăng ký cuộc thi trước khi nộp bài.')
    if (submissionInFlight.current) throw new Error('Lượt nộp trước đang được chấm.')
    submissionInFlight.current = true
    setPending(problem.id); setActionError(''); setLastSubmission(null)
    try {
      const result = await submitContestProblem(contestId, problem.id, language, sourceCode, virtualId)
      setLastSubmission(result)
      await reload()
      return result
    } catch (reason) {
      setActionError(reason instanceof Error ? reason.message : 'Không thể nộp bài. Kiểm tra Bài nộp của tôi trước khi thử lại.')
      void reload()
      throw reason
    } finally { submissionInFlight.current = false; setPending(null) }
  }
  if (loading) return <ContestLoading />
  if (!data) return <section><a href="/contests" className="ui-button-ghost">← Cuộc thi</a><ContestError message={error || 'Không tìm thấy cuộc thi.'} onRetry={() => void reload()} /></section>
  const contest = data.contest
  const chosen = data.problems.find(p => p.id === location.problem)
  const mine = user ? data.myResult : null
  const attemptEnded = Boolean(data.myAttempt && now >= Date.parse(data.myAttempt.endsAt))
  const disabledReason = status !== 'ONGOING'
    ? status === 'FINISHED' ? 'cuộc thi đã kết thúc. Không thể nộp thêm bài.' : 'cuộc thi chưa bắt đầu.'
    : !user ? 'Đăng nhập để nộp bài.' : !data.registered ? 'Đăng ký cuộc thi để nộp bài.'
      : !virtualId && !data.myAttempt ? 'Bấm Bắt đầu làm bài để bắt đầu tính giờ và nộp bài.'
        : attemptEnded ? 'Bạn đã hết thời gian làm bài. Bạn vẫn có thể xem kết quả.' : pending ? 'Đang chấm… Vui lòng chờ kết quả.' : ''
  const lastProblem = data.problems.find(p => p.id === lastSubmission?.problemId)
  return <section className="min-w-0">
    <FlashToast message={toast} onDismiss={() => setToast('')} />
    <a href="/contests" className="ui-button-ghost mb-3 !px-0">← Cuộc thi</a>
    <div className="mb-3 flex flex-wrap gap-2"><a href={`/contests/${contestId}`} className={!virtualId ? 'ui-button-primary' : 'ui-button-secondary'}>Cuộc thi chính thức</a>
      {(virtualId || status === 'FINISHED') && <a href={`/contests/${contestId}/virtual`} className={virtualId ? 'ui-button-primary' : 'ui-button-secondary'}>Thi thử</a>}
      {!virtualId && status === 'FINISHED' && <a className="ui-button-secondary" href={`/contests/${contestId}/results`}>Xem kết quả</a>}</div>
    <header className="mb-5 flex flex-wrap items-center justify-between gap-4">
      <div className="min-w-0"><h1 className="ui-page-title break-words">{contest.name}</h1><p className="mt-2 text-sm text-slate-500">{durationLabel(contest.durationMinutes)} · {contest.participants} người tham gia · {contest.problemCount} bài tập</p></div>
      {!virtualId && status !== 'FINISHED' && (data.registered && user ? <span className="text-sm font-semibold text-blue-600">Đã đăng ký ✓</span> : <button type="button" disabled={registering} onClick={() => void register()} className="ui-button-primary">{registering ? 'Đang đăng ký…' : 'Đăng ký'}</button>)}
    </header>
    {contest.description && <p className="mb-5 whitespace-pre-wrap break-words text-sm leading-6 text-slate-600">{contest.description}</p>}
    {!virtualId && user && <ContestCameraPanel key={`${contestId}:${user.publicId}`} contestId={contestId} userId={user.publicId}
      live={status === 'ONGOING'} registered={data.registered} monitoringEnabled={contest.monitoringEnabled}
      attemptStarted={Boolean(data.myAttempt)} attemptEnded={attemptEnded} onReadyChange={setCameraReady} />}
    {!virtualId && status === 'ONGOING' && user && data.registered && !data.myAttempt && <div className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-lg border border-blue-200 bg-blue-50 p-4">
      <p className="text-sm text-blue-900">Bạn có tối đa {durationLabel(contest.durationMinutes)} từ lúc bắt đầu làm bài, nhưng phải nộp trước giờ kết thúc cuộc thi. Tải lại trang không đặt lại thời gian.</p>
      <button type="button" disabled={starting || (contest.monitoringEnabled && !cameraReady)} onClick={() => void beginAttempt()} className="ui-button-primary shrink-0">{starting ? 'Đang bắt đầu…' : 'Bắt đầu làm bài'}</button>
    </div>}
    <div className="sticky top-[94px] z-30 mb-5 border-y border-slate-200 bg-white shadow-sm lg:top-[53px]">
      <div className="flex flex-wrap items-center justify-between gap-3 px-3 py-3">
        <div className="flex flex-wrap items-center gap-3"><ContestStatusBadge status={status!} /><span role="timer" className="font-mono text-lg font-semibold tabular-nums text-blue-600">
          {status === 'FINISHED' ? 'Cuộc thi đã kết thúc' : status === 'UPCOMING' ? 'Bắt đầu sau ' + countdown(Date.parse(contest.startsAt) - now)
            : attemptEnded ? 'Đã hết thời gian làm bài' : data.myAttempt ? countdown(Date.parse(data.myAttempt.endsAt) - now) + ' làm bài còn lại'
              : countdown(Date.parse(contest.endsAt) - now) + ' đến khi đóng cuộc thi'}
        </span></div>
        <div className="flex items-center gap-3 text-xs"><span className="text-slate-500">{virtualId ? 'Thi thử · ' + (mine?.score ?? 0) + ' điểm' : mine ? 'Hạng của bạn #' + mine.rank + ' · ' + mine.score + ' điểm' : 'Chưa có kết quả cá nhân'}</span><button type="button" disabled={refreshing} onClick={() => void reload()} className="ui-button-ghost">{refreshing ? 'Đang cập nhật…' : 'Làm mới'}</button></div>
      </div>
      <nav className="flex overflow-x-auto" aria-label="Điều hướng cuộc thi">{tabs.map(tab => <button type="button" key={tab.id} onClick={() => navigate(contestId, tab.id)} aria-current={location.tab === tab.id ? 'page' : undefined}
        className={'shrink-0 cursor-pointer border-b-2 px-4 py-3 text-sm font-semibold transition-colors ' + (location.tab === tab.id ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-500 hover:bg-slate-50')}>{virtualId && tab.id === 'leaderboard' ? 'Kết quả cá nhân' : tab.name}</button>)}</nav>
    </div>
    {(error || actionError) && <ContestError message={actionError || error} onRetry={() => { setActionError(''); void reload() }} />}
    {status === 'FINISHED' && <p role="status" className="mb-4 rounded-md border border-slate-200 bg-slate-50 p-3 text-sm">Cuộc thi đã kết thúc. Bạn vẫn có thể xem bài, kết quả và lịch sử nộp.</p>}
    {status === 'ONGOING' && attemptEnded && <p role="status" className="mb-4 rounded-md border border-slate-200 bg-slate-50 p-3 text-sm">Bạn đã hết thời gian làm bài. Không thể nộp thêm hoặc bắt đầu lại lượt thi chính thức.</p>}
    {pending && <p role="status" className="mb-4 rounded-md border border-blue-200 bg-blue-50 p-4 font-medium text-blue-700">Đang chấm… {data.problems.find(p => p.id === pending)?.title}. Bạn có thể chuyển bài trong lúc chờ.</p>}
    {lastSubmission && <div role="status" className={'mb-4 rounded-lg border p-4 text-sm ' + (lastSubmission.status === 'ACCEPTED' ? 'border-blue-200 bg-blue-50 text-blue-800' : 'border-red-200 bg-red-50 text-red-800')}>
      <strong>{verdictLabel(lastSubmission.status)}</strong><div className="mt-2 flex flex-wrap gap-x-5 gap-y-1"><span>{lastProblem?.letter}. {lastProblem?.title}</span><span>{languageLabel(lastSubmission.language)}</span><span>{lastSubmission.executionTimeMillis} ms</span><span>Bộ nhớ: chưa có số liệu</span><time dateTime={lastSubmission.submittedAt}>{new Date(lastSubmission.submittedAt).toLocaleString('vi-VN')}</time></div>
      {lastSubmission.diagnostic && <pre className="mt-2 max-h-32 overflow-auto whitespace-pre-wrap text-xs">{lastSubmission.diagnostic}</pre>}
    </div>}
    {location.tab === 'problems' && <div className="min-w-0">
        {location.problem ? chosen?.available && chosen.slug ? <div className="grid min-w-0 grid-cols-[108px_minmax(0,1fr)] gap-3 sm:grid-cols-[148px_minmax(0,1fr)]">
          <nav aria-label="Điều hướng workspace" className="sticky top-48 self-start overflow-hidden rounded-xl border border-slate-200 bg-white p-1.5 shadow-sm">
            <button type="button" onClick={() => setWorkspaceSideTab(current => current === 'list' ? null : 'list')} aria-pressed={workspaceSideTab === 'list'} aria-expanded={workspaceSideTab === 'list'}
              className={'mb-1 flex min-h-11 w-full cursor-pointer items-center rounded-lg px-2.5 text-left text-xs font-semibold transition-colors sm:text-sm ' + (workspaceSideTab === 'list' ? 'bg-blue-600 text-white' : 'text-slate-600 hover:bg-blue-50 hover:text-blue-700')}>
              Danh sách bài tập
            </button>
            <button type="button" onClick={() => setWorkspaceSideTab(current => current === 'statement' ? null : 'statement')} aria-pressed={workspaceSideTab === 'statement'} aria-expanded={workspaceSideTab === 'statement'}
              className={'flex min-h-11 w-full cursor-pointer items-center rounded-lg px-2.5 text-left text-xs font-semibold transition-colors sm:text-sm ' + (workspaceSideTab === 'statement' ? 'bg-blue-600 text-white' : 'text-slate-600 hover:bg-blue-50 hover:text-blue-700')}>
              Đề bài
            </button>
          </nav>
          <ProblemWorkspace key={String(userId) + chosen.id} slug={chosen.slug} draftCache={drafts} onDraftChange={persist} isolatedDraft={Boolean(virtualId) || chosen.contestOwned} draftNamespace={`contest:${contestId}:${virtualId ?? 'official'}`}
          loadProblem={chosen.contestOwned ? loadContestOwnedProblem : undefined} runTests={chosen.contestOwned ? runContestOwnedProblem : undefined}
          sidePanel={workspaceSideTab === 'list' ? <div className="p-3 sm:p-4">
            <h2 className="mb-3 text-sm font-bold text-slate-950">Danh sách bài tập</h2>
            <div className="grid gap-2">{data.problems.map(p => {
              const solved = p.solved || (lastSubmission?.problemId === p.id && lastSubmission.status === 'ACCEPTED')
              const attempted = data.mySubmissions.some(s => s.problemId === p.id) || lastSubmission?.problemId === p.id
              return <button type="button" key={p.id} disabled={!p.available} onClick={() => navigate(contestId, 'problems', p.id)} aria-current={chosen.id === p.id ? 'page' : undefined}
                className={'cursor-pointer rounded-lg border p-3 text-left text-sm transition-colors disabled:cursor-not-allowed disabled:opacity-50 ' + (chosen.id === p.id ? 'border-blue-300 bg-blue-50 text-blue-800' : 'border-slate-200 hover:border-blue-200 hover:bg-blue-50/60')}>
                <span className="flex items-center justify-between gap-2"><strong>{p.letter}. {p.title}</strong><span aria-label={solved ? 'Đã giải' : attempted ? 'Đã thử' : 'Chưa thử'} className={solved ? 'text-blue-600' : 'text-slate-400'}>{solved ? '✓' : attempted ? '●' : '○'}</span></span>
                <span className="mt-1 block text-xs text-slate-500">{p.points} điểm</span>
              </button>
            })}</div>
          </div> : undefined}
          lockSidePanelOpen
          sidePanelHidden={workspaceSideTab === null}
          compactSidePanel
          onStatementCopy={!virtualId ? () => window.dispatchEvent(new CustomEvent(CONTEST_PROBLEM_COPY_EVENT, { detail: { contestId } })) : undefined}
          onBack={() => navigate(contestId, 'problems')} onAccepted={() => setToast('Chính xác · Đã ghi nhận điểm cuộc thi')}
          submissionPolicy={{ disabledReason, submit }} />
        </div> : <ContestEmpty title="Bài tập không khả dụng" />
          : <div className="overflow-hidden rounded-lg border border-slate-200">{data.problems.map(p => <div key={p.id} className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 p-4 last:border-0">
            <div className="min-w-0"><h2 className="break-words text-sm font-semibold">{p.letter}. {p.title}</h2><p className="mt-1 text-xs text-slate-500">{difficultyLabel(p.difficulty)} · {p.points} điểm · {p.submissions} lượt nộp · {p.solved ? 'Đã giải ✓' : data.mySubmissions.some(s => s.problemId === p.id) ? 'Đã thử ●' : 'Chưa thử ○'}</p></div>
            <button type="button" disabled={!p.available} onClick={() => navigate(contestId, 'problems', p.id)} className="ui-button-secondary">{status === 'ONGOING' ? 'Làm bài' : 'Xem bài'}</button>
          </div>)}{!data.problems.length && <ContestEmpty title="Chưa có bài tập" />}</div>}
    </div>}
    {location.tab === 'leaderboard' && (virtualId ? <ContestResultSummary data={data} virtual /> : <ContestLeaderboard data={data} userId={userId} />)}
    {location.tab === 'submissions' && <ContestSubmissions data={data} signedIn={Boolean(user)} />}
    <p className="mt-4 text-xs text-slate-400">Cập nhật mỗi 5 giây · {new Date(data.serverTime).toLocaleTimeString('vi-VN')}</p>
  </section>
}
