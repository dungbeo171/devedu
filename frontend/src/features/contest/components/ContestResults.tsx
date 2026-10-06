import { useState } from 'react'
import type { ContestDetail } from '../types/contest'
import { countdown, durationLabel, formatStart } from '../contestTime'
import { languageLabel, verdictLabel, matchesSubmissionFilter, type SubmissionFilter } from '../contestPresentation'
import { ContestEmpty } from './ContestStates'

export function ContestLeaderboard({ data, userId }: { data: ContestDetail; userId?: number }) {
  const mine = data.leaderboard.find(row => row.userId === userId)
  return <>
    <p className="mb-3 text-xs text-slate-500">{data.scoringRules.penalty}</p>
    {!data.leaderboard.length ? <ContestEmpty title="No participants yet" /> : <div className="overflow-x-auto rounded-lg border border-slate-200">
      <table className="w-full min-w-[620px] text-left text-sm">
        <caption className="sr-only">Contest leaderboard</caption>
        <thead className="bg-slate-50 text-xs text-slate-500"><tr>
          <th scope="col" className="p-3">Rank</th><th scope="col" className="p-3">User</th><th scope="col" className="p-3">Score</th>
          <th scope="col" className="p-3">{data.scoringRules.wrongAttemptPenaltySeconds ? 'Penalty' : 'Time'}</th>
          {data.problems.map(p => <th scope="col" key={p.id} className="min-w-20 p-3 text-center" title={p.title}>{p.letter}<span className="mt-1 block font-normal">{p.points}</span></th>)}
        </tr></thead>
        <tbody>{data.leaderboard.map(row => <tr key={row.userId} className={`border-t border-slate-100 ${row.userId === userId ? 'bg-blue-50' : ''}`}>
          <td className="p-3 font-mono">#{row.rank}</td><th scope="row" className="min-w-36 max-w-64 break-words p-3 font-medium"><a href={`/profile/user-${row.userId}/contests`} className="hover:text-blue-600 hover:underline">{row.name}</a>{row.userId === userId && <span className="ml-2 text-xs text-blue-600">You</span>}</th>
          <td className="p-3 font-semibold text-blue-600">{row.score}</td><td className="p-3 font-mono text-xs">{countdown(row.timeSeconds * 1000)}</td>
          {row.problems.map(cell => <td key={cell.problemId} className={`p-3 text-center text-xs ${cell.solved ? 'text-blue-600' : cell.attempts ? 'text-red-600' : 'text-slate-400'}`}>
            <span aria-label={cell.solved ? 'Solved' : cell.attempts ? 'Attempted' : 'Not attempted'}>{cell.solved ? '✓' : cell.wrongAttempts ? `−${cell.wrongAttempts}` : '—'}</span>
            {cell.solved && <><strong className="block">+{cell.score}</strong>{cell.wrongAttempts > 0 && <span className="block text-slate-500">−{cell.wrongAttempts} wrong</span>}</>}
          </td>)}
        </tr>)}</tbody>
      </table>
    </div>}
    {mine && <div className="sticky bottom-3 z-10 mt-4 flex flex-wrap items-center gap-x-6 gap-y-2 rounded-lg border border-blue-200 bg-blue-50 p-4 text-sm text-blue-900 shadow-sm">
      <strong>Your Rank #{mine.rank}</strong><span>{mine.score} points</span><span>{mine.solved}/{data.contest.problemCount} solved</span><span className="font-mono">{countdown(mine.timeSeconds * 1000)} {data.scoringRules.wrongAttemptPenaltySeconds ? 'penalty' : 'solve time'}</span>
    </div>}
  </>
}

export function ContestSubmissions({ data, signedIn }: { data: ContestDetail; signedIn: boolean }) {
  const [filter, setFilter] = useState<SubmissionFilter>('All')
  const rows = data.mySubmissions.filter(row => matchesSubmissionFilter(row.status, filter))
  if (!signedIn) return <ContestEmpty title="Sign in to view your submissions"><a href="/login" className="ui-button-primary">Đăng nhập</a></ContestEmpty>
  return <>
    <div className="mb-4 flex flex-wrap gap-2" aria-label="Submission filters">
      {(['All', 'Accepted', 'Wrong Answer', 'Other'] as const).map(value => <button type="button" key={value} aria-pressed={filter === value} onClick={() => setFilter(value)} className={filter === value ? 'ui-button-primary' : 'ui-button-secondary'}>{value}</button>)}
    </div>
    {!rows.length ? <ContestEmpty title="No submissions found">Lượt chạy thử không được tính là submission.</ContestEmpty> : <div className="overflow-x-auto rounded-lg border border-slate-200">
      <table className="w-full min-w-[760px] text-left text-sm"><caption className="sr-only">My submissions</caption>
        <thead className="bg-slate-50 text-xs text-slate-500"><tr>{['Problem', 'Language', 'Verdict', 'Score', 'Runtime', 'Submitted at'].map(t => <th scope="col" key={t} className="p-3">{t}</th>)}</tr></thead>
        <tbody>{rows.map(row => <tr key={row.id} className="border-t border-slate-100">
          <td className="max-w-64 break-words p-3">{row.letter}. {row.title}</td><td className="p-3">{languageLabel(row.language)}</td>
          <td className={`p-3 font-semibold ${row.status === 'ACCEPTED' ? 'text-blue-600' : 'text-red-600'}`}>{verdictLabel(row.status)}</td>
          <td className="p-3">{row.score}/{row.maxScore}</td><td className="whitespace-nowrap p-3 font-mono text-xs">{row.executionTimeMillis} ms</td><td className="whitespace-nowrap p-3 text-xs"><time dateTime={row.submittedAt}>{formatStart(row.submittedAt)}</time></td>
        </tr>)}</tbody>
      </table>
    </div>}
    <p className="mt-3 text-xs text-slate-500">Score là điểm đạt của lượt nộp; tổng điểm mỗi bài chỉ cộng một lần. Runtime đo pha chạy test; Judge hiện chưa trả số liệu bộ nhớ hoặc verdict Memory Limit riêng.</p>
  </>
}

export function ContestRules({ data }: { data: ContestDetail }) {
  const rules = data.scoringRules
  const rows = [
    ['Duration', durationLabel(data.contest.durationMinutes)], ['Start', formatStart(data.contest.startsAt)], ['End', formatStart(data.contest.endsAt)],
    ['Scoring', rules.scoring], ['Penalty', rules.penalty], ['Ranking', rules.ranking],
    ['Attempts', rules.maxAttempts === null ? 'Không giới hạn' : String(rules.maxAttempts)], ['Resubmission', rules.resubmissionAllowed ? 'Được phép' : 'Không được phép'],
  ]
  return <article className="ui-panel p-5 sm:p-6"><h2 className="mb-5 text-lg font-semibold">Contest rules</h2>
    <dl className="divide-y divide-slate-100">{rows.map(([label, value]) => <div key={label} className="grid gap-1 py-3 text-sm sm:grid-cols-[8rem_minmax(0,1fr)]"><dt className="font-semibold">{label}</dt><dd className="leading-6 text-slate-600">{value}</dd></div>)}</dl>
    <p className="mt-5 text-sm leading-7 text-slate-600">Phải đăng ký khi thi Official hoặc bắt đầu phiên Virtual riêng để nộp. Chỉ Submit trong lượt thi mới tính điểm; bài đã giải trước đây và chạy thử không tính. Hạn nộp theo server, không theo đồng hồ máy bạn. Lượt nhận trước hạn vẫn được chấm nếu Judge hoàn tất sau hạn. Các bài thuộc catalog công khai, chưa có chống gian lận.</p>
    <p className="mt-3 text-sm leading-7 text-slate-600">{data.contest.rated ? 'Rated: rating khởi điểm 1200, Elo đa người K=32. Chỉ người có submission chính thức được tính, cần ít nhất 2 người. Kết quả được chốt sau khi Judge hoàn tất, rating cập nhật một lần theo thứ tự thời gian kết thúc.' : 'Unrated: không thay đổi official rating.'} Virtual luôn không ảnh hưởng rating và leaderboard chính thức.</p>
    {data.rules && <section className="mt-5 border-t border-slate-200 pt-5"><h3 className="font-semibold">Organizer rules</h3><p className="mt-2 whitespace-pre-wrap break-words text-sm leading-7 text-slate-600">{data.rules}</p></section>}
  </article>
}
