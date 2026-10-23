import { useEffect, useState } from 'react'
import { getStoredUser } from '../../auth/api/authApi'
import { IconTrophy } from '../../../shared/components/Icons'
import { getProblemLeaderboard } from '../api/problemLeaderboardApi'
import type {
  LeaderboardPeriod,
  ProblemLeaderboard,
  ProblemLeaderboardEntry,
} from '../types/problemLeaderboard'

const periods: { value: LeaderboardPeriod; label: string }[] = [
  { value: 'WEEK', label: 'Tuần' },
  { value: 'MONTH', label: 'Tháng' },
  { value: 'YEAR', label: 'Năm' },
]

function initialPeriod(): LeaderboardPeriod {
  const value = new URLSearchParams(window.location.search).get('period')?.toUpperCase()
  return value === 'MONTH' || value === 'YEAR' ? value : 'WEEK'
}

export function ProblemLeaderboardPage() {
  const [period, setPeriod] = useState<LeaderboardPeriod>(initialPeriod)
  const [data, setData] = useState<ProblemLeaderboard | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [retry, setRetry] = useState(0)
  const currentUserId = getStoredUser()?.publicId

  useEffect(() => {
    const query = new URLSearchParams(window.location.search)
    query.set('period', period.toLowerCase())
    window.history.replaceState(window.history.state, '', `/leaderboard?${query.toString()}`)
    let ignore = false
    setLoading(true)
    setError('')
    void getProblemLeaderboard(period)
      .then(result => { if (!ignore) setData(result) })
      .catch(reason => {
        if (!ignore) setError(reason instanceof Error ? reason.message : 'Không thể tải bảng xếp hạng.')
      })
      .finally(() => { if (!ignore) setLoading(false) })
    return () => { ignore = true }
  }, [period, retry])

  return (
    <section className="mx-auto w-full max-w-6xl overflow-x-hidden">
      <header className="flex flex-col gap-5 border-b border-slate-200 pb-6 sm:flex-row sm:items-end sm:justify-between">
        <div className="max-w-5xl">
          <div className="flex items-center gap-3">
            <span className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-blue-600 text-white shadow-sm shadow-blue-200">
              <IconTrophy className="h-5 w-5" />
            </span>
            <h1 className="font-display text-2xl font-bold tracking-tight text-slate-950 sm:text-3xl">Bảng xếp hạng</h1>
          </div>
          <p className="mt-3 text-sm leading-6 text-slate-600">
            Xếp hạng theo các bài được giải đúng lần đầu trong từng kỳ.
          </p>
        </div>

        <div className="inline-flex w-fit rounded-xl border border-slate-200 bg-slate-100 p-1" role="group" aria-label="Chọn kỳ xếp hạng">
          {periods.map(item => (
            <button
              key={item.value}
              type="button"
              aria-pressed={period === item.value}
              onClick={() => setPeriod(item.value)}
              className={`min-h-9 cursor-pointer rounded-lg px-4 text-sm font-semibold transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 ${
                period === item.value
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'text-slate-600 hover:bg-white hover:text-slate-950'
              }`}
            >
              {item.label}
            </button>
          ))}
        </div>
      </header>

      <div className="mt-5 flex flex-wrap items-center gap-2 text-xs font-semibold">
        <ScoreKey label="Dễ" points={data?.scoring.easy ?? 100} className="border-emerald-200 bg-emerald-50 text-emerald-700" />
        <ScoreKey label="Trung bình" points={data?.scoring.medium ?? 200} className="border-amber-200 bg-amber-50 text-amber-700" />
        <ScoreKey label="Khó" points={data?.scoring.hard ?? 300} className="border-red-200 bg-red-50 text-red-700" />
        {data ? (
          <span className="ml-auto text-slate-500">
            {formatRange(data.startsAt, data.endsAt)}
          </span>
        ) : null}
      </div>

      <div className="mt-5 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-[0_12px_32px_-24px_rgba(15,23,42,.35)]">
        {loading ? <LeaderboardSkeleton /> : error ? (
          <div className="grid min-h-72 place-items-center p-8 text-center">
            <div>
              <p className="font-semibold text-red-700">{error}</p>
              <button type="button" onClick={() => setRetry(value => value + 1)} className="ui-button-primary mt-4 cursor-pointer">Thử lại</button>
            </div>
          </div>
        ) : !data?.entries.length ? (
          <div className="grid min-h-72 place-items-center p-8 text-center">
            <div>
              <IconTrophy className="mx-auto h-9 w-9 text-slate-300" />
              <h2 className="mt-4 text-lg font-bold text-slate-900">Chưa có người được xếp hạng</h2>
              <p className="mt-2 text-sm text-slate-500">Hãy giải đúng một bài tập để xuất hiện trong kỳ này.</p>
            </div>
          </div>
        ) : (
          <>
            <div className="hidden overflow-x-auto md:block">
              <table className="w-full border-collapse text-left">
                <thead className="bg-slate-50 text-xs font-semibold uppercase tracking-wide text-slate-500">
                  <tr>
                    <th className="w-24 px-6 py-4 text-center">Hạng</th>
                    <th className="px-4 py-4">Người học</th>
                    <th className="px-4 py-4 text-center">Đã giải</th>
                    <th className="px-4 py-4 text-center text-emerald-700">Dễ</th>
                    <th className="px-4 py-4 text-center text-amber-700">Trung bình</th>
                    <th className="px-4 py-4 text-center text-red-700">Khó</th>
                    <th className="px-6 py-4 text-right">Điểm</th>
                  </tr>
                </thead>
                <tbody>
                  {data.entries.map(entry => (
                    <LeaderboardRow key={entry.userId} entry={entry} mine={entry.userId === currentUserId} />
                  ))}
                </tbody>
              </table>
            </div>

            <div className="divide-y divide-slate-100 md:hidden">
              {data.entries.map(entry => (
                <LeaderboardCard key={entry.userId} entry={entry} mine={entry.userId === currentUserId} />
              ))}
            </div>
          </>
        )}
      </div>
    </section>
  )
}

function ScoreKey({ label, points, className }: { label: string; points: number; className: string }) {
  return <span className={`rounded-lg border px-2.5 py-1.5 ${className}`}>{label} +{points}</span>
}

function LeaderboardRow({ entry, mine }: { entry: ProblemLeaderboardEntry; mine: boolean }) {
  return (
    <tr className={`border-t border-slate-100 transition-colors hover:bg-blue-50/50 ${mine ? 'bg-blue-50' : ''}`}>
      <td className="px-6 py-4 text-center"><Rank rank={entry.rank} /></td>
      <td className="px-4 py-4">
        <div className="flex items-center gap-3">
          <Avatar name={entry.name} rank={entry.rank} />
          <span className="font-semibold text-slate-900">{entry.name}</span>
          {mine ? <span className="rounded-md bg-blue-600 px-2 py-0.5 text-[10px] font-bold text-white">Bạn</span> : null}
        </div>
      </td>
      <td className="px-4 py-4 text-center font-semibold text-slate-700">{entry.solved}</td>
      <td className="px-4 py-4 text-center font-mono text-sm text-emerald-700">{entry.easySolved}</td>
      <td className="px-4 py-4 text-center font-mono text-sm text-amber-700">{entry.mediumSolved}</td>
      <td className="px-4 py-4 text-center font-mono text-sm text-red-700">{entry.hardSolved}</td>
      <td className="px-6 py-4 text-right font-mono text-base font-bold text-blue-700">{entry.score.toLocaleString('vi-VN')}</td>
    </tr>
  )
}

function LeaderboardCard({ entry, mine }: { entry: ProblemLeaderboardEntry; mine: boolean }) {
  return (
    <article className={`p-4 transition-colors ${mine ? 'bg-blue-50' : 'hover:bg-slate-50'}`}>
      <div className="flex items-center gap-3">
        <Rank rank={entry.rank} />
        <Avatar name={entry.name} rank={entry.rank} />
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <h2 className="truncate font-semibold text-slate-900">{entry.name}</h2>
            {mine ? <span className="rounded-md bg-blue-600 px-2 py-0.5 text-[10px] font-bold text-white">Bạn</span> : null}
          </div>
          <p className="mt-0.5 text-xs text-slate-500">{entry.solved} bài đã giải</p>
        </div>
        <strong className="font-mono text-base text-blue-700">{entry.score.toLocaleString('vi-VN')} đ</strong>
      </div>
      <div className="mt-3 grid grid-cols-3 gap-2 text-center text-xs">
        <span className="rounded-lg bg-emerald-50 px-2 py-2 text-emerald-700">Dễ <b>{entry.easySolved}</b></span>
        <span className="rounded-lg bg-amber-50 px-2 py-2 text-amber-700">Vừa <b>{entry.mediumSolved}</b></span>
        <span className="rounded-lg bg-red-50 px-2 py-2 text-red-700">Khó <b>{entry.hardSolved}</b></span>
      </div>
    </article>
  )
}

function Rank({ rank }: { rank: number }) {
  const style = rank === 1
    ? 'bg-amber-100 text-amber-800 ring-amber-200'
    : rank === 2
      ? 'bg-slate-200 text-slate-700 ring-slate-300'
      : rank === 3
        ? 'bg-orange-100 text-orange-800 ring-orange-200'
        : 'bg-slate-50 text-slate-500 ring-slate-200'
  return <span className={`inline-grid h-8 min-w-8 place-items-center rounded-lg px-2 font-mono text-sm font-bold ring-1 ${style}`}>#{rank}</span>
}

function Avatar({ name, rank }: { name: string; rank: number }) {
  return (
    <span className={`grid h-9 w-9 shrink-0 place-items-center rounded-full text-xs font-bold ${
      rank <= 3 ? 'bg-blue-600 text-white' : 'bg-blue-50 text-blue-700'
    }`}>
      {name.trim().charAt(0).toLocaleUpperCase('vi') || 'U'}
    </span>
  )
}

function LeaderboardSkeleton() {
  return (
    <div className="divide-y divide-slate-100" aria-label="Đang tải bảng xếp hạng">
      {Array.from({ length: 8 }, (_, index) => (
        <div key={index} className="flex animate-pulse items-center gap-4 px-6 py-4">
          <div className="h-8 w-10 rounded-lg bg-slate-100" />
          <div className="h-9 w-9 rounded-full bg-slate-100" />
          <div className="h-4 w-36 rounded bg-slate-100" />
          <div className="ml-auto h-4 w-20 rounded bg-slate-100" />
        </div>
      ))}
    </div>
  )
}

function formatRange(startsAt: string, endsAt: string) {
  const options: Intl.DateTimeFormatOptions = { day: '2-digit', month: '2-digit', year: 'numeric' }
  const start = new Date(startsAt).toLocaleDateString('vi-VN', options)
  const inclusiveEnd = new Date(new Date(endsAt).getTime() - 1)
  return `${start} – ${inclusiveEnd.toLocaleDateString('vi-VN', options)}`
}
