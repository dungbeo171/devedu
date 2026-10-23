import { useCallback, useEffect, useState, type ReactNode } from 'react'
import { getStoredUser } from '../../auth/api/authApi'
import { languageLabel, verdictLabel } from '../../contest/contestPresentation'
import { topicLabels, type ProblemTopic } from '../../programming-problems/types/programmingProblem'
import { IconCheckCircle, IconCode, IconShield, IconTrophy, IconUser } from '../../../shared/components/Icons'
import { getAdminAnalytics } from '../api/adminAnalyticsApi'
import type { AdminAnalytics, Metric } from '../types/adminAnalytics'

const number = new Intl.NumberFormat('vi-VN')

export function AdminAnalyticsPage() {
  const user = getStoredUser()
  const [data, setData] = useState<AdminAnalytics | null>(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(async (refresh = false, signal?: AbortSignal) => {
    if (refresh) setRefreshing(true); else setLoading(true)
    setError('')
    try { setData(await getAdminAnalytics(signal)) }
    catch (reason) {
      if (reason instanceof DOMException && reason.name === 'AbortError') return
      setError(reason instanceof Error ? reason.message : 'Không thể tải thống kê hệ thống.')
    } finally { if (!signal?.aborted) { setLoading(false); setRefreshing(false) } }
  }, [])

  useEffect(() => {
    if (user?.role !== 'ADMIN') { setLoading(false); return }
    const controller = new AbortController()
    void load(false, controller.signal)
    return () => controller.abort()
  }, [load, user?.role])

  if (user?.role !== 'ADMIN') return <ErrorState message="Chỉ ADMIN có thể xem thống kê hệ thống." />
  if (loading) return <AnalyticsSkeleton />
  if (!data) return <ErrorState message={error || 'Không có dữ liệu thống kê.'} onRetry={() => void load()} />

  const submissionRate = percent(data.activity.accepted, data.activity.submissions)
  const runRate = percent(data.activity.successfulTestRuns, data.activity.testRuns)
  return <section className="min-w-0">
    <header className="ui-page-header flex-wrap">
      <div>
        <h1 className="ui-page-title">Thống kê hệ thống</h1>
        <p className="mt-2 text-sm text-slate-500">Cập nhật {new Date(data.generatedAt).toLocaleString('vi-VN')}</p>
      </div>
      <div className="flex flex-wrap gap-2">
        <a href="/admin/users" className="ui-button-secondary">Quản lý tài khoản</a>
        <button type="button" disabled={refreshing} onClick={() => void load(true)} className="ui-button-primary">{refreshing ? 'Đang cập nhật…' : 'Làm mới'}</button>
      </div>
    </header>

    {error && <div role="alert" className="mt-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm font-medium text-red-700">{error}</div>}

    <div className="mt-6 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
      <StatCard icon={<IconUser className="h-5 w-5" />} label="Tài khoản" value={data.users.total} detail={`+${number.format(data.users.newLast7Days)} trong 7 ngày`} />
      <StatCard icon={<IconCode className="h-5 w-5" />} label="Bài tập đang hoạt động" value={data.problems.active} detail={`${number.format(data.problems.deleted)} bài đã xóa mềm`} />
      <StatCard icon={<IconCheckCircle className="h-5 w-5" />} label="Bài nộp" value={data.activity.submissions} detail={`${submissionRate}% được chấp nhận`} />
      <StatCard icon={<IconTrophy className="h-5 w-5" />} label="Kỳ thi" value={data.contests.total} detail={`${number.format(data.contests.ongoing)} đang diễn ra`} />
    </div>

    <div className="mt-5 grid gap-5 xl:grid-cols-[minmax(0,1.7fr)_minmax(300px,0.8fr)]">
      <Panel title="Hoạt động 14 ngày" aside={`${number.format(data.activity.submissionsLast7Days)} bài nộp / 7 ngày`}>
        <ActivityChart data={data.dailyActivity} />
      </Panel>
      <Panel title="Hoạt động học tập">
        <DefinitionRows rows={[
          ['Người từng nộp bài', data.activity.uniqueSubmitters],
          ['Người đã giải bài', data.activity.uniqueSolvers],
          ['Bài nộp 24 giờ', data.activity.submissionsLast24Hours],
          ['Bản nháp đã lưu', data.activity.drafts],
          ['Thời gian chạy trung bình', `${number.format(data.activity.averageRuntimeMillis)} ms`],
          ['Lượt chạy test', data.activity.testRuns],
          ['Chạy test thành công', `${runRate}%`],
        ]} />
      </Panel>
    </div>

    <div className="mt-5 grid gap-5 lg:grid-cols-2 xl:grid-cols-3">
      <Panel title="Người dùng theo vai trò">
        <MetricBars metrics={[
          { key: 'Sinh viên', value: data.users.students },
          { key: 'Giáo viên', value: data.users.teachers },
          { key: 'Quản trị viên', value: data.users.admins },
        ]} />
      </Panel>
      <Panel title="Bài tập theo độ khó">
        <MetricBars metrics={[
          { key: 'Dễ', value: data.problems.easy },
          { key: 'Trung bình', value: data.problems.medium },
          { key: 'Khó', value: data.problems.hard },
        ]} />
      </Panel>
      <Panel title="Chủ đề bài tập">
        <MetricBars metrics={data.topics.map(item => ({ ...item, key: topicName(item.key) }))} limit={8} />
      </Panel>
    </div>

    <div className="mt-5 grid gap-5 lg:grid-cols-2">
      <Panel title="Kết quả chấm bài">
        <MetricBars metrics={data.verdicts.map(item => ({ ...item, key: verdictLabel(item.key) }))} />
      </Panel>
      <Panel title="Ngôn ngữ được sử dụng">
        <MetricBars metrics={data.languages.map(item => ({ ...item, key: languageLabel(item.key) }))} />
      </Panel>
    </div>

    <div className="mt-5 grid gap-5 xl:grid-cols-2">
      <Panel title="Kỳ thi và người tham gia">
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
          <MiniMetric label="Sắp diễn ra" value={data.contests.upcoming} tone="blue" />
          <MiniMetric label="Đang diễn ra" value={data.contests.ongoing} tone="green" />
          <MiniMetric label="Đã kết thúc" value={data.contests.finished} />
          <MiniMetric label="Lượt đăng ký" value={data.contests.registrations} />
          <MiniMetric label="Người tham gia" value={data.contests.uniqueParticipants} />
          <MiniMetric label="Lượt thi thử" value={data.contests.virtualContests} />
        </div>
        <DefinitionRows className="mt-4" rows={[
          ['Công khai', data.contests.publicContests], ['Riêng tư', data.contests.privateContests],
          ['Bản nháp', data.contests.drafts], ['Có giám sát', data.contests.monitored],
          ['Bài nộp trong kỳ thi', data.contests.officialSubmissions],
        ]} />
      </Panel>
      <Panel title="Tín hiệu giám sát" aside={`${number.format(data.monitoring.activeCameras)} camera đang kết nối`}>
        <div className="grid grid-cols-2 gap-3">
          <MiniMetric label="Quay ngang" value={data.monitoring.lookingAway} tone="blue" />
          <MiniMetric label="Chuyển tab" value={data.monitoring.tabHidden} />
          <MiniMetric label="Sao chép đề" value={data.monitoring.problemCopied} />
          <MiniMetric label="Dán nội dung" value={data.monitoring.contentPasted} />
          <MiniMetric label="Chụp màn hình" value={data.monitoring.screenshotAttempts} />
        </div>
        <p className="mt-4 text-xs leading-5 text-slate-500">Tổng {number.format(data.monitoring.behaviorEvents)} tín hiệu hành vi do trình duyệt gửi. Đây là dữ liệu hỗ trợ giám sát, không phải kết luận gian lận.</p>
      </Panel>
    </div>

    <Panel className="mt-5" title="Bài tập có nhiều lượt nộp nhất">
      <TopProblemsTable rows={data.topProblems} />
    </Panel>
  </section>
}

function StatCard({ icon, label, value, detail }: { icon: ReactNode; label: string; value: number; detail: string }) {
  return <article className="rounded-xl border border-slate-200 bg-white p-5">
    <div className="flex items-center justify-between gap-3"><span className="grid h-10 w-10 place-items-center rounded-lg bg-blue-50 text-blue-700">{icon}</span><span className="text-2xl font-bold tabular-nums text-slate-950">{number.format(value)}</span></div>
    <h2 className="mt-4 text-sm font-semibold text-slate-800">{label}</h2><p className="mt-1 text-xs text-slate-500">{detail}</p>
  </article>
}

function Panel({ title, aside, className = '', children }: { title: string; aside?: string; className?: string; children: ReactNode }) {
  return <section className={`rounded-xl border border-slate-200 bg-white p-4 sm:p-5 ${className}`}>
    <header className="mb-5 flex flex-wrap items-center justify-between gap-2"><h2 className="text-sm font-bold text-slate-900">{title}</h2>{aside && <span className="text-xs font-medium text-slate-500">{aside}</span>}</header>
    {children}
  </section>
}

function ActivityChart({ data }: { data: AdminAnalytics['dailyActivity'] }) {
  const maximum = Math.max(1, ...data.map(item => item.submissions))
  return <div className="overflow-x-auto pb-1">
    <div className="flex h-48 min-w-[620px] items-end gap-2 overflow-hidden border-b border-slate-200 px-1" aria-label="Biểu đồ bài nộp trong 14 ngày">
      {data.map(item => <div key={item.date} className="group flex h-full min-w-0 flex-1 flex-col justify-end">
        <div className="relative w-full rounded-t bg-blue-100 transition-colors group-hover:bg-blue-200" style={{ height: `${Math.max(item.submissions ? 5 : 0, item.submissions / maximum * 100)}%` }} title={`${item.submissions} bài nộp, ${item.accepted} accepted`}>
          <div className="absolute inset-x-0 bottom-0 rounded-t bg-blue-600" style={{ height: `${item.submissions ? item.accepted / item.submissions * 100 : 0}%` }} />
        </div>
      </div>)}
    </div>
    <div className="mt-2 flex min-w-[620px] gap-2 px-1 text-center text-[10px] text-slate-400">{data.map((item, index) => <span key={item.date} className="min-w-0 flex-1">{index % 2 === 0 ? new Date(`${item.date}T00:00:00Z`).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' }) : ''}</span>)}</div>
    <div className="mt-4 flex gap-4 text-xs text-slate-500"><span className="flex items-center gap-2"><i className="h-2.5 w-2.5 rounded-sm bg-blue-100" />Tổng bài nộp</span><span className="flex items-center gap-2"><i className="h-2.5 w-2.5 rounded-sm bg-blue-600" />Accepted</span></div>
  </div>
}

function MetricBars({ metrics, limit }: { metrics: Metric[]; limit?: number }) {
  const visible = limit ? metrics.slice(0, limit) : metrics
  const maximum = Math.max(1, ...visible.map(item => item.value))
  if (!visible.length) return <p className="py-8 text-center text-sm text-slate-500">Chưa có dữ liệu.</p>
  return <div className="space-y-4">{visible.map(item => <div key={item.key}>
    <div className="mb-1.5 flex items-center justify-between gap-4 text-xs"><span className="truncate font-medium text-slate-700">{item.key}</span><strong className="tabular-nums text-slate-900">{number.format(item.value)}</strong></div>
    <div className="h-2 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-blue-600" style={{ width: `${item.value / maximum * 100}%` }} /></div>
  </div>)}</div>
}

function DefinitionRows({ rows, className = '' }: { rows: [string, number | string][]; className?: string }) {
  return <dl className={`divide-y divide-slate-100 ${className}`}>{rows.map(([label, value]) => <div key={label} className="flex items-center justify-between gap-4 py-2.5 first:pt-0 last:pb-0"><dt className="text-xs text-slate-500">{label}</dt><dd className="text-sm font-semibold tabular-nums text-slate-900">{typeof value === 'number' ? number.format(value) : value}</dd></div>)}</dl>
}

function MiniMetric({ label, value, tone = 'slate' }: { label: string; value: number; tone?: 'slate' | 'blue' | 'green' }) {
  const color = tone === 'blue' ? 'border-blue-100 bg-blue-50 text-blue-700' : tone === 'green' ? 'border-emerald-100 bg-emerald-50 text-emerald-700' : 'border-slate-200 bg-slate-50 text-slate-700'
  return <div className={`rounded-lg border p-3 ${color}`}><strong className="block text-xl tabular-nums">{number.format(value)}</strong><span className="mt-1 block text-[11px] font-medium">{label}</span></div>
}

function TopProblemsTable({ rows }: { rows: AdminAnalytics['topProblems'] }) {
  if (!rows.length) return <p className="py-8 text-center text-sm text-slate-500">Chưa có bài nộp nào.</p>
  return <div className="overflow-x-auto"><table className="w-full min-w-[620px] text-left text-sm"><thead><tr className="border-b border-slate-200 text-xs text-slate-500"><th className="pb-3 font-semibold">Bài tập</th><th className="pb-3 text-right font-semibold">Bài nộp</th><th className="pb-3 text-right font-semibold">Accepted</th><th className="pb-3 text-right font-semibold">Tỷ lệ</th></tr></thead><tbody>{rows.map(row => <tr key={row.slug} className="border-b border-slate-100 last:border-0"><td className="py-3 pr-4"><a href={`/problems/${row.slug}`} className="font-semibold text-slate-900 transition hover:text-blue-700">{row.title}</a></td><td className="py-3 text-right tabular-nums text-slate-600">{number.format(row.submissions)}</td><td className="py-3 text-right tabular-nums text-blue-700">{number.format(row.accepted)}</td><td className="py-3 text-right font-semibold tabular-nums">{row.acceptanceRate}%</td></tr>)}</tbody></table></div>
}

function AnalyticsSkeleton() {
  return <section aria-label="Đang tải thống kê"><div className="ui-skeleton h-10 w-64 rounded-lg" /><div className="mt-6 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">{Array.from({ length: 4 }, (_, index) => <div key={index} className="ui-skeleton h-36 rounded-xl" />)}</div><div className="mt-5 grid gap-5 xl:grid-cols-2"><div className="ui-skeleton h-72 rounded-xl" /><div className="ui-skeleton h-72 rounded-xl" /></div></section>
}

function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return <section className="grid min-h-[45vh] place-items-center"><div className="max-w-md rounded-xl border border-red-200 bg-red-50 p-6 text-center"><IconShield className="mx-auto h-8 w-8 text-red-600" /><p className="mt-3 text-sm font-medium text-red-700">{message}</p>{onRetry && <button type="button" onClick={onRetry} className="ui-button-secondary mt-4">Thử lại</button>}</div></section>
}

function percent(part: number, total: number) { return total ? Math.round(part * 1000 / total) / 10 : 0 }
function topicName(key: string) { return topicLabels[key as ProblemTopic] ?? key }
