import { useState } from 'react'
import type { ContestDetail } from '../types/contest'
import { countdown, formatStart } from '../contestTime'
import { languageLabel, verdictLabel, matchesSubmissionFilter, scoringRuleLabel, submissionFilterLabels, type SubmissionFilter } from '../contestPresentation'
import { ContestEmpty } from './ContestStates'

export function ContestLeaderboard({ data, userId }: { data: ContestDetail; userId?: number }) {
  const mine = data.leaderboard.find(row => row.userId === userId)
  return <>
    <p className="mb-3 text-xs text-slate-500">{scoringRuleLabel(data.scoringRules.penalty)}</p>
    {!data.leaderboard.length ? <ContestEmpty title="Chưa có người tham gia" /> : <div className="overflow-x-auto rounded-lg border border-slate-200">
      <table className="w-full min-w-[620px] text-left text-sm">
        <caption className="sr-only">Bảng xếp hạng cuộc thi</caption>
        <thead className="bg-slate-50 text-xs text-slate-500"><tr>
          <th scope="col" className="p-3">Thứ hạng</th><th scope="col" className="p-3">Người tham gia</th><th scope="col" className="p-3">Điểm</th>
          <th scope="col" className="p-3">{data.scoringRules.wrongAttemptPenaltySeconds ? 'Thời gian phạt' : 'Thời gian'}</th>
          {data.problems.map(p => <th scope="col" key={p.id} className="min-w-20 p-3 text-center" title={p.title}>{p.letter}<span className="mt-1 block font-normal">{p.points}</span></th>)}
        </tr></thead>
        <tbody>{data.leaderboard.map(row => <tr key={row.userId} className={`border-t border-slate-100 ${row.userId === userId ? 'bg-blue-50' : ''}`}>
          <td className="p-3 font-mono">#{row.rank}</td><th scope="row" className="min-w-36 max-w-64 break-words p-3 font-medium"><a href={`/profile/user-${row.userId}/contests`} className="hover:text-blue-600 hover:underline">{row.name}</a>{row.userId === userId && <span className="ml-2 text-xs text-blue-600">Bạn</span>}</th>
          <td className="p-3 font-semibold text-blue-600">{row.score}</td><td className="p-3 font-mono text-xs">{countdown(row.timeSeconds * 1000)}</td>
          {row.problems.map(cell => <td key={cell.problemId} className={`p-3 text-center text-xs ${cell.solved ? 'text-blue-600' : cell.attempts ? 'text-red-600' : 'text-slate-400'}`}>
            <span aria-label={cell.solved ? 'Đã giải' : cell.attempts ? 'Đã thử' : 'Chưa thử'}>{cell.solved ? '✓' : cell.wrongAttempts ? `−${cell.wrongAttempts}` : '—'}</span>
            {cell.solved && <><strong className="block">+{cell.score}</strong>{cell.wrongAttempts > 0 && <span className="block text-slate-500">−{cell.wrongAttempts} lượt sai</span>}</>}
          </td>)}
        </tr>)}</tbody>
      </table>
    </div>}
    {mine && <div className="sticky bottom-3 z-10 mt-4 flex flex-wrap items-center gap-x-6 gap-y-2 rounded-lg border border-blue-200 bg-blue-50 p-4 text-sm text-blue-900 shadow-sm">
      <strong>Hạng của bạn #{mine.rank}</strong><span>{mine.score} điểm</span><span>{mine.solved}/{data.contest.problemCount} bài đã giải</span><span className="font-mono">{countdown(mine.timeSeconds * 1000)} {data.scoringRules.wrongAttemptPenaltySeconds ? 'thời gian phạt' : 'thời gian giải'}</span>
    </div>}
  </>
}

export function ContestSubmissions({ data, signedIn }: { data: ContestDetail; signedIn: boolean }) {
  const [filter, setFilter] = useState<SubmissionFilter>('All')
  const rows = data.mySubmissions.filter(row => matchesSubmissionFilter(row.status, filter))
  if (!signedIn) return <ContestEmpty title="Đăng nhập để xem bài nộp của bạn"><a href="/login" className="ui-button-primary">Đăng nhập</a></ContestEmpty>
  return <>
    <div className="mb-4 flex flex-wrap gap-2" aria-label="Lọc bài nộp">
      {(['All', 'Accepted', 'Wrong Answer', 'Other'] as const).map(value => <button type="button" key={value} aria-pressed={filter === value} onClick={() => setFilter(value)} className={filter === value ? 'ui-button-primary' : 'ui-button-secondary'}>{submissionFilterLabels[value]}</button>)}
    </div>
    {!rows.length ? <ContestEmpty title="Chưa có bài nộp phù hợp">Lượt chạy thử không được tính là bài nộp.</ContestEmpty> : <div className="overflow-x-auto rounded-lg border border-slate-200">
      <table className="w-full min-w-[760px] text-left text-sm"><caption className="sr-only">Bài nộp của tôi</caption>
        <thead className="bg-slate-50 text-xs text-slate-500"><tr>{['Bài tập', 'Ngôn ngữ', 'Kết quả', 'Điểm', 'Thời gian chạy', 'Thời điểm nộp'].map(t => <th scope="col" key={t} className="p-3">{t}</th>)}</tr></thead>
        <tbody>{rows.map(row => <tr key={row.id} className="border-t border-slate-100">
          <td className="max-w-64 break-words p-3">{row.letter}. {row.title}</td><td className="p-3">{languageLabel(row.language)}</td>
          <td className={`p-3 font-semibold ${row.status === 'ACCEPTED' ? 'text-blue-600' : 'text-red-600'}`}>{verdictLabel(row.status)}</td>
          <td className="p-3">{row.score}/{row.maxScore}</td><td className="whitespace-nowrap p-3 font-mono text-xs">{row.executionTimeMillis} ms</td><td className="whitespace-nowrap p-3 text-xs"><time dateTime={row.submittedAt}>{formatStart(row.submittedAt)}</time></td>
        </tr>)}</tbody>
      </table>
    </div>}
    <p className="mt-3 text-xs text-slate-500">Điểm của lượt nộp chỉ được cộng một lần cho mỗi bài. Thời gian chạy đo giai đoạn chạy bộ kiểm thử; hệ thống chấm chưa trả số liệu bộ nhớ hoặc kết quả vượt giới hạn bộ nhớ riêng.</p>
  </>
}
