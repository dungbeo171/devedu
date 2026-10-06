import type { ContestStatus } from '../types/contest'
export function ContestStatusBadge({ status }: { status: ContestStatus }) {
  return <span className={`inline-flex items-center gap-1.5 rounded-md border px-2 py-1 text-xs font-semibold ${status === 'ONGOING' ? 'border-blue-200 bg-blue-50 text-blue-700' : status === 'UPCOMING' ? 'border-slate-200 bg-white text-slate-600' : 'border-slate-200 bg-slate-100 text-slate-500'}`}>
    {status === 'ONGOING' && <span className="h-1.5 w-1.5 rounded-full bg-blue-600" />}
    {status === 'ONGOING' ? 'Live' : status === 'UPCOMING' ? 'Upcoming' : 'Finished'}
  </span>
}
