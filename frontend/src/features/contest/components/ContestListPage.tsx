import { useEffect, useMemo, useState } from 'react'
import { getStoredUser } from '../../auth/api/authApi'
import type { ContestCatalog, ContestStatus, ContestSummary, ManagedContestCatalog } from '../types/contest'
import { useContestResource } from '../hooks/useContestResource'
import { contestStatus } from '../contestTime'
import { ContestEmpty, ContestError, ContestLoading } from './ContestStates'
import { ContestRow } from './ContestRow'

type StatusFilter = ContestStatus | 'ALL'
const statusOptions: { value: StatusFilter; label: string }[] = [
  { value: 'ALL', label: 'Tất cả' },
  { value: 'ONGOING', label: 'Đang diễn ra' },
  { value: 'UPCOMING', label: 'Sắp diễn ra' },
  { value: 'FINISHED', label: 'Đã kết thúc' },
]
const statusPriority: Record<ContestStatus, number> = { ONGOING: 0, UPCOMING: 1, FINISHED: 2 }

function initialStatus(): StatusFilter {
  const value = new URLSearchParams(window.location.search).get('status')
  return statusOptions.some(option => option.value === value) ? value as StatusFilter : 'ALL'
}

export function ContestListPage() {
  const user = getStoredUser()
  const canManage = user?.role === 'ADMIN' || user?.role === 'TEACHER'
  const [filter, setFilter] = useState<StatusFilter>(initialStatus)
  const upcoming = useContestResource<ContestCatalog>('/api/contests?status=UPCOMING&sort=START_TIME')
  const ongoing = useContestResource<ContestCatalog>('/api/contests?status=ONGOING&sort=START_TIME')
  const finished = useContestResource<ContestCatalog>('/api/contests?status=FINISHED&sort=START_TIME')
  const loading = ongoing.loading || upcoming.loading || finished.loading
  const refreshing = ongoing.refreshing || upcoming.refreshing || finished.refreshing
  const error = ongoing.error || upcoming.error || finished.error
  const now = Math.max(ongoing.now, upcoming.now, finished.now)

  const contests = useMemo(() => {
    const unique = new Map<string, ContestSummary>()
    for (const resource of [ongoing, upcoming, finished]) {
      for (const contest of resource.data?.contests ?? []) unique.set(contest.id, contest)
    }
    return [...unique.values()]
      .filter(contest => filter === 'ALL' || contestStatus(contest, now) === filter)
      .sort((left, right) => {
        const leftStatus = contestStatus(left, now), rightStatus = contestStatus(right, now)
        const byStatus = statusPriority[leftStatus] - statusPriority[rightStatus]
        if (byStatus) return byStatus
        if (leftStatus === 'FINISHED') return Date.parse(right.endsAt) - Date.parse(left.endsAt)
        return Date.parse(left.startsAt) - Date.parse(right.startsAt)
      })
  }, [filter, finished.data, ongoing.data, upcoming.data, now])

  const transitioned = [ongoing, upcoming, finished].some(resource => resource.data?.contests.some(contest => contestStatus(contest, now) !== contest.status))
  useEffect(() => {
    if (transitioned) void Promise.all([ongoing.reload(), upcoming.reload(), finished.reload()])
  }, [transitioned, ongoing.reload, upcoming.reload, finished.reload])

  function reloadAll() {
    return Promise.all([ongoing.reload(), upcoming.reload(), finished.reload()])
  }
  function changeFilter(next: StatusFilter) {
    setFilter(next)
    const url = new URL(window.location.href)
    if (next === 'ALL') url.searchParams.delete('status'); else url.searchParams.set('status', next)
    window.history.replaceState({}, '', url)
  }

  return <section>
    <header className="ui-page-header flex-wrap">
      <div><h1 className="ui-page-title">Cuộc thi</h1></div>
      {canManage && <div className="flex flex-wrap gap-2">
        <a className="ui-button-secondary" href="/admin">Quản lý kỳ thi</a>
        <a className="ui-button-primary" href="/contests/create">+ Tạo cuộc thi</a>
      </div>}
    </header>

    <div className="mt-5 flex flex-wrap items-end justify-between gap-3 border-b border-slate-200 pb-4">
      <label className="block text-xs font-semibold text-slate-600">Trạng thái
        <select className="ui-control mt-1.5 !w-48 cursor-pointer" value={filter} onChange={event => changeFilter(event.target.value as StatusFilter)}>
          {statusOptions.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
        </select>
      </label>
      <button type="button" onClick={() => void reloadAll()} disabled={refreshing} className="ui-button-ghost">{refreshing ? 'Đang cập nhật…' : 'Làm mới'}</button>
    </div>

    {error && <ContestError message={error} onRetry={() => void reloadAll()} />}
    {loading ? <ContestLoading /> : !error && !contests.length
      ? <ContestEmpty title="Chưa có cuộc thi phù hợp">Hãy chọn trạng thái khác hoặc quay lại sau.</ContestEmpty>
      : <PublicContestCards contests={contests} now={now} role={user?.role} />}
  </section>
}

function PublicContestCards({ contests, now, role }: { contests: ContestSummary[]; now: number; role?: string }) {
  if (role === 'ADMIN') return <ContestRows contests={contests} now={now} reviewableIds={new Set(contests.filter(contest => contest.monitoringEnabled).map(contest => contest.id))} />
  if (role === 'TEACHER') return <TeacherContestCards contests={contests} now={now} />
  return <ContestRows contests={contests} now={now} reviewableIds={new Set()} />
}

function TeacherContestCards({ contests, now }: { contests: ContestSummary[]; now: number }) {
  const owned = useContestResource<ManagedContestCatalog>('/api/teacher/contests/mine')
  const supervised = useContestResource<ManagedContestCatalog>('/api/teacher/contests/supervised')
  const loading = owned.loading || supervised.loading
  const reviewableIds = new Set([
    ...(owned.data?.contests ?? []),
    ...(supervised.data?.contests ?? []),
  ].filter(contest => contest.monitoringEnabled).map(contest => contest.id))
  return <ContestRows contests={contests} now={now} reviewableIds={reviewableIds} checkingPermissions={loading} />
}

function ContestRows({ contests, now, reviewableIds, checkingPermissions = false }: { contests: ContestSummary[]; now: number; reviewableIds: Set<string>; checkingPermissions?: boolean }) {
  return <div className="mt-5 space-y-3">
    {contests.map(contest => {
      const status = contestStatus(contest, now)
      const canReview = reviewableIds.has(contest.id)
      const action = canReview
        ? <a className="ui-button-primary min-w-24 justify-center whitespace-nowrap" href={`/contests/${contest.id}?monitoring=1`}>Giám sát</a>
        : checkingPermissions && contest.monitoringEnabled
          ? <span className="ui-button-secondary min-w-24 justify-center whitespace-nowrap opacity-60">Đang kiểm tra…</span>
          : <a className={`${status === 'ONGOING' ? 'ui-button-primary' : 'ui-button-secondary'} min-w-20 justify-center whitespace-nowrap`} href={`/contests/${contest.id}${status === 'FINISHED' ? '/results' : status === 'ONGOING' ? '?enter=1' : ''}`}>
            {status === 'UPCOMING' ? 'Xem' : status === 'ONGOING' ? 'Vào thi' : 'Kết quả'}
          </a>
      return <ContestRow key={contest.id} contest={contest} now={now} action={action} />
    })}
  </div>
}
