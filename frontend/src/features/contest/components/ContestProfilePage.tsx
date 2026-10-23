import { useContestResource } from '../hooks/useContestResource'
import type { ContestProfile } from '../types/contest'
import { ContestEmpty, ContestError, ContestLoading } from './ContestStates'
import { formatStart } from '../contestTime'

export function ContestProfilePage({ handle }: { handle: string }) {
  const { data, error, loading, reload } = useContestResource<ContestProfile>(`/api/profiles/${encodeURIComponent(handle)}/contests`)
  const history = data?.history ?? []
  return <section>{error && <ContestError message={error} onRetry={() => void reload()} />}{loading ? <ContestLoading /> : data && <>
    <header className="ui-page-header"><div><h1 className="ui-page-title">{data.name}</h1><p className="mt-2 text-sm text-slate-500">@{data.handle}</p></div></header>
    <h2 className="my-5 text-lg font-semibold">Lịch sử thi đấu</h2>
    <div className="mb-5 grid grid-cols-2 gap-3 md:grid-cols-4">{[
      ['Số cuộc thi đã tham gia', data.participated], ['Số lần thắng', data.wins], ['Trong 10 hạng đầu', data.top10], ['Trong 100 hạng đầu', data.top100],
      ['Thứ hạng cao nhất', data.bestRank ? '#' + data.bestRank : '—'], ['Thứ hạng trung bình', data.participated ? data.averageRank.toFixed(1) : '—'], ['Số lượt thi thử', data.virtualContests],
    ].map(([label, value]) => <div key={label} className="rounded-lg border border-slate-200 p-4"><p className="text-xs text-slate-500">{label}</p><strong className="mt-2 block text-xl tabular-nums">{value}</strong></div>)}</div>
    {!history.length ? <ContestEmpty title="Chưa có lịch sử" /> : <div className="overflow-x-auto rounded-lg border border-slate-200"><table className="w-full min-w-[520px] text-left text-sm"><thead className="bg-slate-50"><tr>{['cuộc thi', 'Thứ hạng', 'Điểm', 'Ngày kết thúc'].map(t => <th key={t} scope="col" className="p-3">{t}</th>)}</tr></thead><tbody>{history.map(h => <tr key={h.contestId} className="border-t border-slate-100"><td className="p-3"><a className="text-blue-600 hover:underline" href={`/contests/${h.contestId}/results`}>{h.name}</a></td><td className="p-3">#{h.rank}</td><td className="p-3">{h.score}</td><td className="p-3 text-xs">{formatStart(h.endedAt)}</td></tr>)}</tbody></table></div>}
    {data.virtualHistory.length > 0 && <section className="mt-6"><h2 className="mb-3 text-lg font-semibold">Lịch sử thi thử của bạn</h2><div className="divide-y divide-slate-100 rounded-lg border border-slate-200">{data.virtualHistory.map(v => <a key={v.id} href={`/contests/${v.contestId}/virtual?session=${v.id}`} className="flex flex-wrap justify-between gap-2 p-4 text-sm hover:bg-slate-50"><span className="text-blue-600">{v.name}</span><span>{formatStart(v.startsAt)}</span></a>)}</div></section>}
  </>}</section>
}
