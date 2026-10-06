import { useEffect, useRef, useState } from 'react'
import { getStoredUser } from '../../auth/api/authApi'
import { ProblemWorkspace } from '../../programming-problems/components/ProblemWorkspace'
import type { ProblemSubmission } from '../../programming-problems/types/programmingProblem'
import { FlashToast } from '../../../shared/components/FlashToast'
import { registerContest, submitContestProblem } from '../api/contestApi'
import type { ContestDetail, ContestSubmit, ContestResult } from '../types/contest'
import { useContestDrafts } from '../hooks/useContestDrafts'
import { ContestResultSummary } from './ContestResultSummary'
import { useContestResource } from '../hooks/useContestResource'
import { contestStatus, countdown, durationLabel } from '../contestTime'
import { verdictLabel, languageLabel } from '../contestPresentation'
import { ContestStatusBadge } from './ContestStatusBadge'
import { ContestEmpty, ContestError, ContestLoading } from './ContestStates'
import { ContestLeaderboard, ContestSubmissions, ContestRules } from './ContestResults'

type Tab = 'problems' | 'leaderboard' | 'submissions' | 'rules'
const tabs: { id: Tab; name: string }[] = [
  { id: 'problems', name: 'Problems' }, { id: 'leaderboard', name: 'Leaderboard' },
  { id: 'submissions', name: 'My Submissions' }, { id: 'rules', name: 'Rules' },
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
  const [actionError, setActionError] = useState('')
  const [toast, setToast] = useState('')
  const [pending, setPending] = useState<string | null>(null)
  const [lastSubmission, setLastSubmission] = useState<ProblemSubmission | null>(null)
  const submissionInFlight = useRef(false)
  const user = getStoredUser()
  const userId = user?.publicId
  const { drafts, persist } = useContestDrafts(String(userId) + ':' + contestId + ':' + (virtualId ?? 'official'))
  const status = data ? contestStatus(data.contest, now) : null
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
    try { await registerContest(contestId); await reload(); setToast('Đăng ký Contest thành công') }
    catch (reason) { setActionError(reason instanceof Error ? reason.message : 'Không thể đăng ký.') }
    finally { setRegistering(false) }
  }
  const submit: ContestSubmit = async (slug, language, sourceCode) => {
    const problem = data?.problems.find(p => p.slug === slug)
    if (!problem || !data?.registered) throw new Error('Đăng ký Contest trước khi nộp bài.')
    if (submissionInFlight.current) throw new Error('Lượt nộp trước đang được chấm.')
    submissionInFlight.current = true
    setPending(problem.id); setActionError(''); setLastSubmission(null)
    try {
      const result = await submitContestProblem(contestId, problem.id, language, sourceCode, virtualId)
      setLastSubmission(result)
      await reload()
      return result
    } catch (reason) {
      setActionError(reason instanceof Error ? reason.message : 'Không thể nộp bài. Kiểm tra My Submissions trước khi thử lại.')
      void reload()
      throw reason
    } finally { submissionInFlight.current = false; setPending(null) }
  }
  if (loading) return <ContestLoading />
  if (!data) return <section><a href="/contests" className="ui-button-ghost">← Contests</a><ContestError message={error || 'Không tìm thấy Contest.'} onRetry={() => void reload()} /></section>
  const contest = data.contest
  const chosen = data.problems.find(p => p.id === location.problem)
  const mine = user ? data.myResult : null
  const disabledReason = status !== 'ONGOING'
    ? status === 'FINISHED' ? 'Contest đã kết thúc. Không thể nộp thêm bài.' : 'Contest chưa bắt đầu.'
    : !user ? 'Đăng nhập để nộp bài.' : !data.registered ? 'Đăng ký Contest để nộp bài.' : pending ? 'Judging... Vui lòng chờ kết quả.' : ''
  const lastProblem = data.problems.find(p => p.id === lastSubmission?.problemId)
  return <section className="min-w-0">
    <FlashToast message={toast} onDismiss={() => setToast('')} />
    <a href="/contests" className="ui-button-ghost mb-3 !px-0">← Contests</a>
    <div className="mb-3 flex flex-wrap gap-2"><a href={`/contests/${contestId}`} className={!virtualId ? 'ui-button-primary' : 'ui-button-secondary'}>Official Contest</a>
      {(virtualId || status === 'FINISHED') && <a href={`/contests/${contestId}/virtual`} className={virtualId ? 'ui-button-primary' : 'ui-button-secondary'}>Virtual Contest</a>}
      {!virtualId && status === 'FINISHED' && <a className="ui-button-secondary" href={`/contests/${contestId}/results`}>View Results</a>}</div>
    <header className="mb-5 flex flex-wrap items-center justify-between gap-4">
      <div className="min-w-0"><h1 className="ui-page-title break-words">{contest.name}</h1><p className="mt-2 text-sm text-slate-500">{durationLabel(contest.durationMinutes)} · {contest.participants} participants · {contest.problemCount} problems</p></div>
      {!virtualId && status !== 'FINISHED' && (data.registered && user ? <span className="text-sm font-semibold text-blue-600">Registered ✓</span> : <button type="button" disabled={registering} onClick={() => void register()} className="ui-button-primary">{registering ? 'Registering…' : 'Register'}</button>)}
    </header>
    <div className="sticky top-[94px] z-30 mb-5 border-y border-slate-200 bg-white shadow-sm lg:top-[53px]">
      <div className="flex flex-wrap items-center justify-between gap-3 px-3 py-3">
        <div className="flex flex-wrap items-center gap-3"><ContestStatusBadge status={status!} /><span role="timer" className="font-mono text-lg font-semibold tabular-nums text-blue-600">
          {status === 'FINISHED' ? 'Contest ended' : status === 'UPCOMING' ? 'Starts in ' + countdown(Date.parse(contest.startsAt) - now) : countdown(Date.parse(contest.endsAt) - now) + ' remaining'}
        </span></div>
        <div className="flex items-center gap-3 text-xs"><span className="text-slate-500">{virtualId ? 'Virtual · ' + (mine?.score ?? 0) + ' pts' : mine ? 'Your Rank #' + mine.rank + ' · ' + mine.score + ' pts' : data.contest.rated ? 'Rated' : 'Unrated'}</span><button type="button" disabled={refreshing} onClick={() => void reload()} className="ui-button-ghost">{refreshing ? 'Updating…' : 'Refresh'}</button></div>
      </div>
      <nav className="flex overflow-x-auto" aria-label="Contest navigation">{tabs.map(tab => <button type="button" key={tab.id} onClick={() => navigate(contestId, tab.id)} aria-current={location.tab === tab.id ? 'page' : undefined}
        className={'shrink-0 cursor-pointer border-b-2 px-4 py-3 text-sm font-semibold transition-colors ' + (location.tab === tab.id ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-500 hover:bg-slate-50')}>{virtualId && tab.id === 'leaderboard' ? 'Personal Result' : tab.name}</button>)}</nav>
    </div>
    {(error || actionError) && <ContestError message={actionError || error} onRetry={() => { setActionError(''); void reload() }} />}
    {status === 'FINISHED' && <p role="status" className="mb-4 rounded-md border border-slate-200 bg-slate-50 p-3 text-sm">Contest ended. Bạn vẫn có thể xem bài, kết quả và lịch sử nộp.</p>}
    {pending && <p role="status" className="mb-4 rounded-md border border-blue-200 bg-blue-50 p-4 font-medium text-blue-700">Judging… {data.problems.find(p => p.id === pending)?.title}. Bạn có thể chuyển bài trong lúc chờ.</p>}
    {lastSubmission && <div role="status" className={'mb-4 rounded-lg border p-4 text-sm ' + (lastSubmission.status === 'ACCEPTED' ? 'border-blue-200 bg-blue-50 text-blue-800' : 'border-red-200 bg-red-50 text-red-800')}>
      <strong>{verdictLabel(lastSubmission.status)}</strong><div className="mt-2 flex flex-wrap gap-x-5 gap-y-1"><span>{lastProblem?.letter}. {lastProblem?.title}</span><span>{languageLabel(lastSubmission.language)}</span><span>{lastSubmission.executionTimeMillis} ms</span><span>Memory: chưa có số liệu</span><time dateTime={lastSubmission.submittedAt}>{new Date(lastSubmission.submittedAt).toLocaleString('vi-VN')}</time></div>
      {lastSubmission.diagnostic && <pre className="mt-2 max-h-32 overflow-auto whitespace-pre-wrap text-xs">{lastSubmission.diagnostic}</pre>}
    </div>}
    {location.tab === 'problems' && <div className="grid min-w-0 gap-4 xl:grid-cols-[190px_minmax(0,1fr)]">
      <aside className="min-w-0"><nav aria-label="Contest problems" className="flex gap-2 overflow-x-auto xl:sticky xl:top-48 xl:max-h-[65vh] xl:flex-col xl:overflow-y-auto">
        {data.problems.map(p => {
          const solved = p.solved || (lastSubmission?.problemId === p.id && lastSubmission.status === 'ACCEPTED')
          const attempted = data.mySubmissions.some(s => s.problemId === p.id) || lastSubmission?.problemId === p.id
          return <button type="button" key={p.id} disabled={!p.available} onClick={() => navigate(contestId, 'problems', p.id)} aria-current={chosen?.id === p.id ? 'page' : undefined}
            className={'min-w-36 shrink-0 cursor-pointer rounded-md border p-3 text-left text-sm transition-colors disabled:cursor-not-allowed disabled:opacity-50 xl:min-w-0 ' + (chosen?.id === p.id ? 'border-blue-200 bg-blue-50 text-blue-700' : 'border-slate-200 hover:bg-slate-50')}>
            <span className="flex justify-between gap-3"><strong>{p.letter}</strong><span aria-label={solved ? 'Solved' : attempted ? 'Attempted' : 'Not attempted'} className={solved ? 'text-blue-600' : 'text-slate-500'}>{solved ? '✓' : attempted ? '●' : '○'}</span></span><span className="mt-1 block break-words font-medium">{p.title}</span><span className="mt-1 block text-xs text-slate-500">{p.points} pts</span>
          </button>
        })}
      </nav></aside>
      <div className="min-w-0">
        {location.problem ? chosen?.available && chosen.slug ? <ProblemWorkspace key={String(userId) + chosen.id} slug={chosen.slug} draftCache={drafts} onDraftChange={persist} isolatedDraft={Boolean(virtualId)}
          onBack={() => navigate(contestId, 'problems')} onAccepted={() => setToast('Accepted · Đã ghi nhận điểm Contest')}
          submissionPolicy={{ disabledReason, submit }} /> : <ContestEmpty title="Problem unavailable" />
          : <div className="overflow-hidden rounded-lg border border-slate-200">{data.problems.map(p => <div key={p.id} className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 p-4 last:border-0">
            <div className="min-w-0"><h2 className="break-words text-sm font-semibold">{p.letter}. {p.title}</h2><p className="mt-1 text-xs text-slate-500">{p.difficulty} · {p.points} pts · {p.submissions} submissions · {p.solved ? 'Solved ✓' : data.mySubmissions.some(s => s.problemId === p.id) ? 'Attempted ●' : 'Not attempted ○'}</p></div>
            <button type="button" disabled={!p.available} onClick={() => navigate(contestId, 'problems', p.id)} className="ui-button-secondary">{status === 'ONGOING' ? 'Solve' : 'View'}</button>
          </div>)}{!data.problems.length && <ContestEmpty title="No problems yet" />}</div>}
      </div>
    </div>}
    {location.tab === 'leaderboard' && (virtualId ? <ContestResultSummary data={data} rating={null} virtual /> : <ContestLeaderboard data={data} userId={userId} />)}
    {location.tab === 'submissions' && <ContestSubmissions data={data} signedIn={Boolean(user)} />}
    {location.tab === 'rules' && <ContestRules data={data} />}
    <p className="mt-4 text-xs text-slate-400">Cập nhật mỗi 5 giây · {new Date(data.serverTime).toLocaleTimeString('vi-VN')}</p>
  </section>
}
