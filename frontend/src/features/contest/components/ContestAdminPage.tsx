import { useState } from 'react'
import { getStoredUser } from '../../auth/api/authApi'
import { deleteContest } from '../api/contestApi'
import { FlashToast } from '../../../shared/components/FlashToast'
import type { ManagedContestCatalog } from '../types/contest'
import { useContestResource } from '../hooks/useContestResource'
import { ContestEmpty, ContestError, ContestLoading } from './ContestStates'
import { ContestRow } from './ContestRow'

export function ContestAdminPage() {
  const user = getStoredUser()
  const canManage = user?.role === 'ADMIN' || user?.role === 'TEACHER'
  const owned = useContestResource<ManagedContestCatalog>('/api/teacher/contests/mine')
  const supervised = useContestResource<ManagedContestCatalog>('/api/teacher/contests/supervised')
  const [flash, setFlash] = useState('')

  if (!canManage) return <section className="rounded-lg border border-red-200 bg-red-50 p-5 text-sm text-red-700">Bạn không có quyền quản lý kỳ thi.</section>
  const contests = [...(owned.data?.contests ?? [])].sort((left, right) => Date.parse(right.createdAt) - Date.parse(left.createdAt))
  const supervisedContests = [...(supervised.data?.contests ?? [])].sort((left, right) => Date.parse(right.createdAt) - Date.parse(left.createdAt))
  const now = Math.max(owned.now, supervised.now)
  return <section>
    <FlashToast message={flash} onDismiss={() => setFlash('')} />
    <header className="ui-page-header flex-wrap">
      <div><a href="/contests" className="ui-button-ghost mb-2 !px-0">← Cuộc thi</a><h1 className="ui-page-title">Quản lý kỳ thi</h1></div>
      <div className="flex flex-wrap gap-2">
        {user?.role === 'ADMIN' && <a href="/admin/statistics" className="ui-button-secondary">Thống kê hệ thống</a>}
        {user?.role === 'ADMIN' && <a href="/admin/users" className="ui-button-secondary">Quản lý tài khoản</a>}
        <a href="/contests/create" className="ui-button-primary">+ Tạo cuộc thi</a>
      </div>
    </header>
    <div className="mt-5 flex justify-end border-b border-slate-200 pb-4"><button type="button" disabled={owned.refreshing || supervised.refreshing} onClick={() => void Promise.all([owned.reload(), supervised.reload()])} className="ui-button-ghost">{owned.refreshing || supervised.refreshing ? 'Đang cập nhật…' : 'Làm mới'}</button></div>
    {owned.error && <ContestError message={owned.error} onRetry={() => void owned.reload()} />}
    <h2 className="mt-5 text-sm font-bold text-slate-900">{user?.role === 'ADMIN' ? 'Tất cả kỳ thi' : 'Kỳ thi của tôi'}</h2>
    {owned.loading ? <ContestLoading /> : !owned.error && !contests.length ? <ContestEmpty title="Bạn chưa tạo kỳ thi nào"><a href="/contests/create" className="ui-button-primary mt-4">+ Tạo cuộc thi</a></ContestEmpty> : null}
    <div className="mt-5 space-y-3">
      {contests.map(contest => <ContestRow key={contest.id} contest={contest} now={now}
        badges={<span className={`rounded-md border px-2 py-0.5 text-[10px] font-semibold ${contest.published ? contest.visibility === 'PUBLIC' ? 'border-blue-200 bg-blue-50 text-blue-700' : 'border-slate-200 bg-slate-50 text-slate-600' : 'border-amber-200 bg-amber-50 text-amber-700'}`}>{contest.published ? contest.visibility === 'PUBLIC' ? 'Công khai' : 'Riêng tư' : 'Bản nháp'}</span>}
        action={<ContestActions contestId={contest.id} contestName={contest.name} onDeleted={async () => { await owned.reload(); setFlash('Đã xóa kỳ thi') }} />} />)}
    </div>
    {user?.role === 'TEACHER' && <section className="mt-8 border-t border-slate-200 pt-6">
      <h2 className="text-sm font-bold text-slate-900">Kỳ thi được phân công giám sát</h2>
      <p className="mt-1 text-xs text-slate-500">Bạn chỉ xem báo cáo giám sát, không thể chỉnh sửa các kỳ thi này.</p>
      {supervised.error && <ContestError message={supervised.error} onRetry={() => void supervised.reload()} />}
      {supervised.loading ? <ContestLoading /> : !supervised.error && !supervisedContests.length ? <ContestEmpty title="Chưa có kỳ thi được phân công" /> : null}
      <div className="mt-4 space-y-3">{supervisedContests.map(contest => <ContestRow key={contest.id} contest={contest} now={now}
        badges={<span className="rounded-md border border-blue-200 bg-blue-50 px-2 py-0.5 text-[10px] font-semibold text-blue-700">Giám sát</span>}
        action={<a href={`/contests/${contest.id}?monitoring=1`} className="ui-button-primary whitespace-nowrap">Xem thống kê</a>} />)}</div>
    </section>}
  </section>
}

function ContestActions({ contestId, contestName, onDeleted }: { contestId: string; contestName: string; onDeleted: () => Promise<void> }) {
  const [deleting, setDeleting] = useState(false)
  async function remove() {
    if (deleting || !window.confirm(`Xóa kỳ thi "${contestName}"? Kỳ thi sẽ không còn xuất hiện trong hệ thống.`)) return
    setDeleting(true)
    try { await deleteContest(contestId); await onDeleted() }
    finally { setDeleting(false) }
  }
  return <details className="group relative z-20">
    <summary aria-label="Mở thao tác kỳ thi" title="Thao tác" className="grid h-10 w-10 cursor-pointer list-none place-items-center rounded-lg border border-slate-200 bg-white text-2xl leading-none text-slate-600 transition hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700 [&::-webkit-details-marker]:hidden">⋯</summary>
    <div className="absolute right-0 top-12 z-30 w-44 rounded-lg border border-slate-200 bg-white p-1.5 shadow-[0_12px_28px_rgba(15,23,42,.14)]">
      <a href={`/contests/${contestId}/edit`} className="flex min-h-9 items-center rounded-md px-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-100">Chỉnh sửa</a>
      <a href={`/contests/${contestId}?monitoring=1`} className="flex min-h-9 items-center rounded-md px-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-100">Giám sát</a>
      <button type="button" disabled={deleting} onClick={() => void remove()} className="flex min-h-9 w-full cursor-pointer items-center rounded-md px-3 text-left text-sm font-semibold text-red-700 transition hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50">{deleting ? 'Đang xóa…' : 'Xóa kỳ thi'}</button>
    </div>
  </details>
}
