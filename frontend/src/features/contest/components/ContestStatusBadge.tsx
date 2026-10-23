import type { ContestStatus } from '../types/contest'
export function ContestStatusBadge({ status }: { status: ContestStatus }) {
  return <span className={`inline-flex items-center gap-1.5 rounded-md border px-2.5 py-1 text-xs font-semibold ${status === 'ONGOING' ? 'border-emerald-200 bg-emerald-500 text-white' : status === 'UPCOMING' ? 'border-blue-200 bg-blue-50 text-blue-700' : 'border-slate-200 bg-slate-100 text-slate-600'}`}>
    {status === 'ONGOING' && <span className="h-1.5 w-1.5 rounded-full bg-white" />}
    {status === 'ONGOING' ? 'Đang diễn ra' : status === 'UPCOMING' ? 'Sắp diễn ra' : 'Đã kết thúc'}
  </span>
}
