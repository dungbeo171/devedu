import { useEffect, useState } from 'react'
import { getStoredUser } from '../../auth/api/authApi'
import { setPendingFlash } from '../../../shared/flashMessage'
import { IconCalendar, IconClock } from '../../../shared/components/Icons'
import type { ContestCatalog, ContestStatus } from '../types/contest'
import { useContestResource } from '../hooks/useContestResource'
import { contestStatus, countdown, startsIn, endedAgo, durationLabel, formatStart } from '../contestTime'
import { ContestStatusBadge } from './ContestStatusBadge'
import { ContestEmpty, ContestError, ContestLoading } from './ContestStates'
import { CreateContestDialog } from './CreateContestDialog'

const tabs: { value: ContestStatus; label: string }[] = [
  { value: 'UPCOMING', label: 'Upcoming' }, { value: 'ONGOING', label: 'Ongoing' }, { value: 'FINISHED', label: 'Finished' },
]
const types = { WEEKLY: 'Weekly', PRACTICE: 'Practice', CUSTOM: 'Custom' }
export function ContestListPage() {
  const [tab, setTab] = useState<ContestStatus>('UPCOMING')
  const [creating, setCreating] = useState(false)
  const [ratingFilter, setRatingFilter] = useState('ALL')
  const [difficulty, setDifficulty] = useState('')
  const [sort, setSort] = useState('START_TIME')
  const params = new URLSearchParams({ status: ratingFilter === 'VIRTUAL' ? 'FINISHED' : tab, sort })
  if (ratingFilter === 'RATED' || ratingFilter === 'UNRATED') params.set('rated', String(ratingFilter === 'RATED'))
  if (difficulty) params.set('difficulty', difficulty)
  const { data, error, loading, refreshing, now, reload } = useContestResource<ContestCatalog>(`/api/contests?${params}`)
  const user = getStoredUser()
  const canCreate = user?.role === 'ADMIN' || user?.role === 'TEACHER'
  const transitioned = data?.contests.some(c => contestStatus(c, now) !== c.status) ?? false
  useEffect(() => { if (transitioned) void reload() }, [transitioned, reload])
  const visible = data?.contests.filter(c => contestStatus(c, now) === (ratingFilter === 'VIRTUAL' ? 'FINISHED' : tab)) ?? []
  return <section>
    <header className="ui-page-header flex-wrap">
      <div><h1 className="ui-page-title">Contests</h1></div>
      {canCreate && <button type="button" className="ui-button-primary" onClick={() => setCreating(true)}>+ Create Contest</button>}
    </header>
    <div className="mt-5 flex flex-wrap items-center justify-between gap-3 border-b border-slate-200">
      <nav aria-label="Contest status" className="flex gap-1 overflow-x-auto">
        {tabs.map(item => <button key={item.value} type="button" disabled={ratingFilter === 'VIRTUAL'} aria-pressed={(ratingFilter === 'VIRTUAL' ? 'FINISHED' : tab) === item.value} onClick={() => setTab(item.value)}
          className={`cursor-pointer border-b-2 px-4 py-3 text-sm font-semibold transition-colors ${tab === item.value ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-500 hover:text-slate-900'}`}>{item.label}</button>)}
      </nav>
      <button type="button" onClick={() => void reload()} disabled={refreshing} className="ui-button-ghost">{refreshing ? 'Updating…' : 'Refresh'}</button>
    </div>
    <div className="my-4 flex flex-wrap items-end gap-3">
      <label className="text-xs text-slate-500">Type<select className="ui-control mt-1 !w-auto" value={ratingFilter} onChange={e => { setRatingFilter(e.target.value); if (e.target.value === 'VIRTUAL') setTab('FINISHED') }}><option value="ALL">All types</option><option value="RATED">Rated</option><option value="UNRATED">Unrated</option><option value="VIRTUAL">Virtual available</option></select></label>
      <label className="text-xs text-slate-500">Difficulty<select className="ui-control mt-1 !w-auto" value={difficulty} onChange={e => setDifficulty(e.target.value)}><option value="">All difficulties</option><option value="BEGINNER">Beginner</option><option value="INTERMEDIATE">Intermediate</option><option value="ADVANCED">Advanced</option></select></label>
      <label className="text-xs text-slate-500">Sort<select className="ui-control mt-1 !w-auto" value={sort} onChange={e => setSort(e.target.value)}><option value="START_TIME">Start time</option><option value="PARTICIPANTS">Participants</option><option value="POPULARITY">Popularity (official submissions)</option></select></label>
      {user && <a className="ui-button-secondary sm:ml-auto" href={`/profile/user-${user.publicId}/contests`}>My Contest Profile</a>}
    </div>
    {error && <ContestError message={error} onRetry={() => void reload()} />}
    {loading ? <ContestLoading /> : !error && !visible.length ? <ContestEmpty title={`No ${tabs.find(t => t.value === tab)?.label.toLowerCase()} contests`}>Contest mới sẽ xuất hiện tại đây khi được tạo.</ContestEmpty> : null}
    <div className="mt-5 grid gap-4 md:grid-cols-2">
      {visible.map(contest => <article key={contest.id} className="ui-card flex min-w-0 flex-col p-5">
        <div className="flex items-center justify-between gap-3"><span className="text-xs font-medium text-slate-500">{types[contest.type]} · {contest.rated ? 'Rated' : 'Unrated'} · {contest.difficulty.toLowerCase()}</span><ContestStatusBadge status={contestStatus(contest, now)} /></div>
        <h2 className="mt-4 break-words text-lg font-semibold text-slate-950">{contest.name}</h2>
        <div className="mt-4 space-y-2 text-sm text-slate-500">
          <p className="flex items-center gap-2"><IconCalendar className="h-4 w-4 shrink-0" /><time dateTime={contest.startsAt}>{formatStart(contest.startsAt)}</time></p>
          <p className="flex items-center gap-2"><IconClock className="h-4 w-4 shrink-0" />{durationLabel(contest.durationMinutes)} · {contest.problemCount} Problems</p>
          <p>{contest.participants.toLocaleString()} participants</p>
        </div>
        <div className="mt-5 flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 pt-4">
          <span className={`text-xs ${tab === 'ONGOING' ? 'font-mono font-semibold tabular-nums text-blue-600' : 'text-slate-500'}`}>
            {tab === 'ONGOING' ? `${countdown(Date.parse(contest.endsAt) - now)} remaining` : tab === 'UPCOMING' ? `Starts in ${startsIn(Date.parse(contest.startsAt) - now)}` : endedAgo(now - Date.parse(contest.endsAt))}
          </span>
          <a className={tab === 'ONGOING' ? 'ui-button-primary' : 'ui-button-secondary'} href={`/contests/${contest.id}${ratingFilter === 'VIRTUAL' ? '/virtual' : tab === 'FINISHED' ? '/results' : tab === 'ONGOING' ? '?enter=1' : ''}`}>
            {ratingFilter === 'VIRTUAL' ? 'Start Virtual Contest' : tab === 'UPCOMING' ? 'View Contest' : tab === 'ONGOING' ? 'Enter Contest' : 'View Results'}
          </a>
        </div>
      </article>)}
    </div>
    {creating && <CreateContestDialog onClose={() => setCreating(false)} onCreated={id => { setPendingFlash('Tạo Contest thành công'); window.location.assign(`/contests/${id}`) }} />}
  </section>
}
