import { useContestResource } from '../hooks/useContestResource'
import type { ContestProfile } from '../types/contest'
import { ContestEmpty, ContestError, ContestLoading } from './ContestStates'
import { RatingDelta } from './ContestResultSummary'
import { formatStart } from '../contestTime'

export function ContestProfilePage({ handle, view }: { handle: string; view: 'contests' | 'rating' }) {
  const { data, error, loading, reload } = useContestResource<ContestProfile>(`/api/profiles/${encodeURIComponent(handle)}/contests`)
  const history = data?.history.filter(h => view !== 'rating' || h.rating !== null) ?? []
  return <section>{error && <ContestError message={error} onRetry={() => void reload()} />}{loading ? <ContestLoading /> : data && <>
    <header className="ui-page-header"><div><h1 className="ui-page-title">{data.name}</h1><p className="mt-2 text-sm text-slate-500">@{data.handle}</p></div></header>
    <nav className="my-5 flex gap-2"><a className={view === 'contests' ? 'ui-button-primary' : 'ui-button-secondary'} href={`/profile/${handle}/contests`}>Contest History</a><a className={view === 'rating' ? 'ui-button-primary' : 'ui-button-secondary'} href={`/profile/${handle}/rating`}>Rating History</a></nav>
    <div className="mb-5 grid grid-cols-2 gap-3 md:grid-cols-4">{[
      ['Current rating', data.currentRating], ['Previous rating', data.previousRating], ['Change', (data.ratingChange > 0 ? '+' : '') + data.ratingChange], ['Peak rating', data.peakRating],
      ['Contests participated', data.participated], ['Wins', data.wins], ['Top 10', data.top10], ['Top 100', data.top100],
      ['Best rank', data.bestRank ? '#' + data.bestRank : '—'], ['Average rank', data.participated ? data.averageRank.toFixed(1) : '—'], ['Virtual contests', data.virtualContests],
    ].map(([label, value]) => <div key={label} className="rounded-lg border border-slate-200 p-4"><p className="text-xs text-slate-500">{label}</p><strong className="mt-2 block text-xl tabular-nums">{value}</strong></div>)}</div>
    <p className="mb-4 text-xs leading-6 text-slate-500">Rating khởi điểm 1200. Elo đa người, K=32; chỉ tính Rated có ít nhất 2 người nộp bài. Virtual và đăng ký không nộp bài không ảnh hưởng rating.</p>
    {!history.length ? <ContestEmpty title="Chưa có lịch sử" /> : <div className="overflow-x-auto rounded-lg border border-slate-200"><table className="w-full min-w-[680px] text-left text-sm"><thead className="bg-slate-50"><tr>{['Contest', 'Rank', 'Score', 'Rating Change', 'Date'].map(t => <th key={t} scope="col" className="p-3">{t}</th>)}</tr></thead><tbody>{history.map(h => <tr key={h.contestId} className="border-t border-slate-100"><td className="p-3"><a className="text-blue-600 hover:underline" href={`/contests/${h.contestId}/results`}>{h.name}</a></td><td className="p-3">#{h.rank}</td><td className="p-3">{h.score}</td><td className="p-3">{h.rating ? <RatingDelta rating={h.rating} /> : '—'}</td><td className="p-3 text-xs">{formatStart(h.endedAt)}</td></tr>)}</tbody></table></div>}
    {view === 'contests' && data.virtualHistory.length > 0 && <section className="mt-6"><h2 className="mb-3 text-lg font-semibold">Your virtual history</h2><div className="divide-y divide-slate-100 rounded-lg border border-slate-200">{data.virtualHistory.map(v => <a key={v.id} href={`/contests/${v.contestId}/virtual?session=${v.id}`} className="flex flex-wrap justify-between gap-2 p-4 text-sm hover:bg-slate-50"><span className="text-blue-600">{v.name}</span><span>{formatStart(v.startsAt)}</span></a>)}</div></section>}
  </>}</section>
}
