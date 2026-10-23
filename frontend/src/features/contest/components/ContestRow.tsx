import type { ReactNode } from 'react'
import { IconBookOpen, IconClock, IconUser } from '../../../shared/components/Icons'
import type { ContestSummary } from '../types/contest'
import { contestStatus, countdown, endedAgo, startsIn } from '../contestTime'
import { ContestStatusBadge } from './ContestStatusBadge'

type Props = {
  contest: ContestSummary
  now: number
  badges?: ReactNode
  action: ReactNode
}

export function ContestRow({ contest, now, badges, action }: Props) {
  const status = contestStatus(contest, now)
  const timing = status === 'ONGOING'
    ? `${countdown(Date.parse(contest.endsAt) - now)} còn lại`
    : status === 'UPCOMING'
      ? `Bắt đầu sau ${startsIn(Date.parse(contest.startsAt) - now)}`
      : endedAgo(now - Date.parse(contest.endsAt))

  return <article className="group relative z-0 overflow-visible rounded-xl border border-slate-200 bg-white px-4 py-4 transition duration-200 hover:-translate-y-px hover:border-blue-200 hover:shadow-[0_8px_24px_rgba(15,23,42,.06)] focus-within:z-50 has-[details[open]]:z-50 sm:px-5">
    <div className="grid min-w-0 grid-cols-2 gap-x-4 gap-y-4 xl:grid-cols-[minmax(290px,1.7fr)_210px_84px_120px_86px_132px_auto] xl:items-center xl:gap-x-5">
      <div className="col-span-2 flex min-w-0 items-start gap-3 xl:col-span-1">
        <span className="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-blue-600 text-white shadow-[0_5px_14px_rgba(37,99,235,.2)] transition-transform duration-300 group-hover:scale-105">
          <IconBookOpen className="h-4 w-4" />
        </span>
        <div className="min-w-0">
          <h2 className="truncate text-[15px] font-bold leading-5 text-slate-950" title={contest.name}>{contest.name}</h2>
          <div className="mt-2 flex flex-wrap items-center gap-1.5">
            {contest.monitoringEnabled && <span className="inline-flex items-center gap-1 rounded-md border border-amber-200 bg-amber-50 px-2 py-0.5 text-[10px] font-semibold text-amber-700"><span className="h-1.5 w-1.5 rounded-full bg-amber-500" />Giám sát</span>}
            {contest.participationMode === 'RESTRICTED' && <span className="rounded-md border border-violet-200 bg-violet-50 px-2 py-0.5 text-[10px] font-semibold text-violet-700">Giới hạn email</span>}
            {badges}
            <span className="text-[10px] font-medium text-slate-400">Tạo {formatDateOnly(contest.createdAt)}</span>
          </div>
        </div>
      </div>

      <div className="col-span-2 grid gap-2 text-xs tabular-nums text-slate-700 sm:grid-cols-2 xl:col-span-1 xl:grid-cols-1" aria-label="Lịch kỳ thi">
        <TimePoint tone="start" value={contest.startsAt} label="Bắt đầu" />
        <TimePoint tone="end" value={contest.endsAt} label="Kết thúc" />
      </div>

      <Metric tone="blue" label="Thời gian làm"><IconClock className="h-3.5 w-3.5" /><strong>{contest.durationMinutes}p</strong></Metric>
      <Metric tone="violet" label="Số bài tập"><span className="grid h-6 w-6 place-items-center rounded-full bg-violet-500 text-[10px] font-bold text-white">B</span><strong>{contest.problemCount} bài</strong></Metric>
      <Metric tone="emerald" label="Người tham gia"><IconUser className="h-3.5 w-3.5" /><strong>{contest.participants.toLocaleString('vi-VN')}</strong></Metric>

      <div className="min-w-0 self-center">
        <ContestStatusBadge status={status} />
        <p className="mt-1.5 truncate text-[10px] font-medium text-slate-400" title={timing}>{timing}</p>
      </div>
      <div className="flex items-center justify-end self-center">{action}</div>
    </div>
  </article>
}

function TimePoint({ tone, value, label }: { tone: 'start' | 'end'; value: string; label: string }) {
  const formatted = formatDateTime(value)
  return <div className="flex min-w-0 items-center gap-2" title={`${label}: ${formatted}`}>
    <span className={`grid h-5 w-5 shrink-0 place-items-center rounded ${tone === 'start' ? 'bg-emerald-100' : 'bg-rose-100'}`}>
      <span className={`h-1.5 w-1.5 rounded-full ${tone === 'start' ? 'bg-emerald-500' : 'bg-rose-500'}`} />
    </span>
    <time className="truncate font-medium" dateTime={value}>{formatted}</time>
  </div>
}

function Metric({ tone, label, children }: { tone: 'blue' | 'violet' | 'emerald'; label: string; children: ReactNode }) {
  const colors = tone === 'blue' ? 'bg-blue-50 text-blue-700' : tone === 'violet' ? 'bg-violet-50 text-violet-700' : 'bg-emerald-50 text-emerald-700'
  return <div aria-label={label} title={label} className={`inline-flex min-h-9 w-fit items-center gap-1.5 rounded-full px-3 text-xs ${colors}`}>{children}</div>
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false }).format(new Date(value))
}

function formatDateOnly(value: string) {
  return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(new Date(value))
}
