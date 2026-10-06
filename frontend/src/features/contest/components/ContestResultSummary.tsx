import type { ContestDetail, RatingChange } from '../types/contest'
import { countdown } from '../contestTime'
import { ContestEmpty } from './ContestStates'

export function RatingDelta({ rating }: { rating: RatingChange }) {
  return <span className="font-mono">{rating.previousRating} → {rating.newRating} <strong className={rating.ratingChange < 0 ? 'text-red-600' : 'text-blue-600'}>({rating.ratingChange > 0 ? '+' : ''}{rating.ratingChange})</strong></span>
}
export function ContestResultSummary({ data, rating, virtual = false }: { data: ContestDetail; rating: RatingChange | null; virtual?: boolean }) {
  const mine = data.myResult
  if (!mine || !data.mySubmissions.length) return <ContestEmpty title="Chưa có kết quả cá nhân">Bạn chưa nộp bài trong lượt thi này. Đăng ký nhưng không nộp bài không tính là tham gia xếp hạng/rating.</ContestEmpty>
  const wrong = mine.problems.reduce((sum, p) => sum + p.wrongAttempts, 0)
  const average = mine.solved ? mine.problems.reduce((sum, p) => sum + (p.solvedAtSeconds ?? 0), 0) / mine.solved : 0
  const metrics = [[virtual ? 'Mode' : 'Rank', virtual ? 'Virtual · Personal' : '#' + mine.rank], ['Score', mine.score],
    [data.scoringRules.wrongAttemptPenaltySeconds ? 'Penalty' : 'Solve time', countdown(mine.timeSeconds * 1000)], ['Solved', mine.solved + ' / ' + data.contest.problemCount],
    ['Wrong attempts', wrong], ['Average solve time', countdown(average * 1000)]]
  return <div className="space-y-5">
    <div className="grid grid-cols-2 gap-3 lg:grid-cols-3">{metrics.map(([name, value]) => <div key={name} className="rounded-lg border border-slate-200 p-4"><p className="text-xs text-slate-500">{name}</p><p className="mt-2 text-xl font-semibold tabular-nums">{value}</p></div>)}</div>
    <div className="rounded-lg border border-blue-100 bg-blue-50 p-4 text-sm"><strong className="mr-3">Rating</strong>{virtual ? 'Official rating không thay đổi.' : rating ? <RatingDelta rating={rating} /> : data.contest.rated ? 'Không tính rating nếu ít hơn 2 người đã nộp bài.' : 'Unrated · Rating không thay đổi.'}</div>
    {mine.solved === data.contest.problemCount && <p className="text-sm font-semibold text-blue-600">Bạn đã giải tất cả bài tập trong Contest.</p>}
    <section><h2 className="mb-3 text-lg font-semibold">Problem performance</h2><div className="divide-y divide-slate-100 rounded-lg border border-slate-200">{data.problems.map(p => {
      const result = mine.problems.find(r => r.problemId === p.id)
      return <div key={p.id} className="flex flex-wrap items-center justify-between gap-3 p-4 text-sm"><div className="min-w-0"><h3 className="break-words font-medium">{p.letter}. {p.title}</h3><p className={result?.solved ? 'mt-1 text-blue-600' : 'mt-1 text-slate-500'}>{result?.solved ? 'Solved' : 'Not solved'}</p></div><div className="text-right font-mono"><p>{result?.score ?? 0} / {p.points}</p><p className="mt-1 text-xs text-slate-500">{result?.solvedAtSeconds == null ? '—' : countdown(result.solvedAtSeconds * 1000)}</p></div></div>
    })}</div></section>
    <p className="text-xs leading-6 text-slate-500">Average solve time là trung bình thời điểm giải từng bài tính từ lúc bắt đầu. Wrong attempts đếm lượt sai trước Accepted đầu tiên; không tự thêm penalty vào quy tắc hiện tại.</p>
  </div>
}
