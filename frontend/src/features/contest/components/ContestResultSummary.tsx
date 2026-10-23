import type { ContestDetail } from '../types/contest'
import { countdown } from '../contestTime'
import { ContestEmpty } from './ContestStates'

export function ContestResultSummary({ data, virtual = false }: { data: ContestDetail; virtual?: boolean }) {
  const mine = data.myResult
  if (!mine || !data.mySubmissions.length) return <ContestEmpty title="Chưa có kết quả cá nhân">Bạn chưa nộp bài trong lượt thi này. Đăng ký nhưng không nộp bài không tính là tham gia xếp hạng.</ContestEmpty>
  const wrong = mine.problems.reduce((sum, p) => sum + p.wrongAttempts, 0)
  const average = mine.solved ? mine.problems.reduce((sum, p) => sum + (p.solvedAtSeconds ?? 0), 0) / mine.solved : 0
  const metrics = [[virtual ? 'Chế độ' : 'Thứ hạng', virtual ? 'Thi thử · Cá nhân' : '#' + mine.rank], ['Điểm', mine.score],
    [data.scoringRules.wrongAttemptPenaltySeconds ? 'Thời gian phạt' : 'Thời gian giải', countdown(mine.timeSeconds * 1000)], ['Đã giải', mine.solved + ' / ' + data.contest.problemCount],
    ['Số lượt sai', wrong], ['Thời gian giải trung bình', countdown(average * 1000)]]
  return <div className="space-y-5">
    <div className="grid grid-cols-2 gap-3 lg:grid-cols-3">{metrics.map(([name, value]) => <div key={name} className="rounded-lg border border-slate-200 p-4"><p className="text-xs text-slate-500">{name}</p><p className="mt-2 text-xl font-semibold tabular-nums">{value}</p></div>)}</div>
    {mine.solved === data.contest.problemCount && <p className="text-sm font-semibold text-blue-600">Bạn đã giải tất cả bài tập trong cuộc thi.</p>}
    <section><h2 className="mb-3 text-lg font-semibold">Kết quả từng bài</h2><div className="divide-y divide-slate-100 rounded-lg border border-slate-200">{data.problems.map(p => {
      const result = mine.problems.find(r => r.problemId === p.id)
      return <div key={p.id} className="flex flex-wrap items-center justify-between gap-3 p-4 text-sm"><div className="min-w-0"><h3 className="break-words font-medium">{p.letter}. {p.title}</h3><p className={result?.solved ? 'mt-1 text-blue-600' : 'mt-1 text-slate-500'}>{result?.solved ? 'Đã giải' : 'Chưa giải'}</p></div><div className="text-right font-mono"><p>{result?.score ?? 0} / {p.points}</p><p className="mt-1 text-xs text-slate-500">{result?.solvedAtSeconds == null ? '—' : countdown(result.solvedAtSeconds * 1000)}</p></div></div>
    })}</div></section>
    <p className="text-xs leading-6 text-slate-500">Thời gian giải trung bình tính từ lúc bắt đầu đến khi giải đúng từng bài. Số lượt sai chỉ đếm trước lần giải đúng đầu tiên; quy tắc hiện tại không cộng thêm thời gian phạt.</p>
  </div>
}
