import { useEffect, useState } from 'react'
import { ModalDialog } from '../../../shared/components/ModalDialog'
import { contestRequest } from '../api/contestApi'
import { useContestResource } from '../hooks/useContestResource'
import type { CameraReport, CameraTimeline } from './types'

type ReportTab = 'LOOKING_AWAY' | 'NO_FACE' | 'TAB_HIDDEN' | 'PROBLEM_COPIED' | 'CONTENT_PASTED' | 'SCREENSHOT_ATTEMPT'
type CameraParticipant = CameraReport['participants'][number]
const reportTabs: { id: ReportTab; label: string; count: (row: CameraParticipant) => number; latest: (row: CameraParticipant) => string | null }[] = [
  { id: 'LOOKING_AWAY', label: 'Quay ngang', count: row => row.lookingAwayCount, latest: row => row.lastLookingAwayAt },
  { id: 'NO_FACE', label: 'Rời khung hình', count: row => row.noFaceCount, latest: row => row.lastNoFaceAt },
  { id: 'TAB_HIDDEN', label: 'Chuyển tab', count: row => row.tabHiddenCount, latest: row => row.lastTabHiddenAt },
  { id: 'PROBLEM_COPIED', label: 'Sao chép', count: row => row.problemCopiedCount, latest: row => row.lastProblemCopiedAt },
  { id: 'CONTENT_PASTED', label: 'Dán nội dung', count: row => row.contentPastedCount, latest: row => row.lastContentPastedAt },
  { id: 'SCREENSHOT_ATTEMPT', label: 'Chụp màn hình', count: row => row.screenshotAttemptCount, latest: row => row.lastScreenshotAttemptAt },
]

function cameraStatus(lastEvent: string | null, lastReceivedAt: string | null, attemptStartedAt: string | null, now: number) {
  if (!attemptStartedAt) return { label: 'Chưa bắt đầu làm bài', tone: 'bg-blue-50 text-blue-700' }
  if (!lastReceivedAt) return { label: 'Chưa kết nối camera', tone: 'bg-slate-100 text-slate-600' }
  if (lastEvent === 'CAMERA_STOPPED') return { label: 'Camera đã ngắt', tone: 'bg-red-50 text-red-700' }
  const age = now - new Date(lastReceivedAt).getTime()
  if (age >= 0 && age <= 25000) return { label: 'Camera đang kết nối', tone: 'bg-emerald-50 text-emerald-700' }
  return { label: 'Mất tín hiệu camera', tone: 'bg-amber-50 text-amber-700' }
}

export function CameraReportDialog({ contestId, onClose, embedded = false }: { contestId: string; onClose?: () => void; embedded?: boolean }) {
  const [page, setPage] = useState(1)
  const [tab, setTab] = useState<ReportTab>('LOOKING_AWAY')
  const [timelineTarget, setTimelineTarget] = useState<{ userId: number; name: string } | null>(null)
  const { data, error, loading, now, reload } = useContestResource<CameraReport>(`/api/teacher/contests/${encodeURIComponent(contestId)}/camera?page=${page}`, 10000)
  const selected = reportTabs.find(item => item.id === tab)!
  const cameraTab = tab === 'LOOKING_AWAY' || tab === 'NO_FACE'
  const content = <>
    <div className="mb-4 flex gap-1 overflow-x-auto border-b border-slate-200" role="tablist" aria-label="Loại hành vi">
      {reportTabs.map(item => <button key={item.id} type="button" role="tab" aria-selected={tab === item.id} onClick={() => { setTab(item.id); setTimelineTarget(null) }}
        className={'shrink-0 cursor-pointer border-b-2 px-3 py-2.5 text-xs font-semibold transition-colors ' + (tab === item.id ? 'border-blue-600 text-blue-700' : 'border-transparent text-slate-500 hover:bg-slate-50 hover:text-slate-800')}>
        {item.label}
      </button>)}
    </div>
    {loading && <p role="status">Đang tải thống kê…</p>}
    {error && <div role="alert" className="text-sm text-red-700">{error} <button className="ui-button-secondary" onClick={() => void reload()}>Thử lại</button></div>}
    {data && <><div className="overflow-x-auto"><table className="w-full text-left text-sm"><thead><tr className="border-b border-slate-200"><th className="p-2">Người tham gia</th><th className="p-2">{selected.label}</th>{cameraTab && <th className="p-2">Camera</th>}<th className="p-2">Vi phạm gần nhất</th></tr></thead>
      <tbody>{data.participants.map(p => {
        const status = cameraStatus(p.lastEvent, p.lastReceivedAt, p.attemptStartedAt, now)
        const count = selected.count(p)
        const latest = selected.latest(p)
        return <tr key={p.userId} className="border-b border-slate-100"><td className="p-2 font-medium">{p.name}</td><td className="p-2"><strong className="tabular-nums text-blue-700">{count}</strong>{count > 0 && <button type="button" onClick={() => setTimelineTarget({ userId: p.userId, name: p.name })} className="ml-3 cursor-pointer text-xs font-semibold text-blue-700 underline-offset-2 hover:underline">Xem thời gian</button>}</td>{cameraTab && <td className="p-2 text-xs"><span className={'inline-flex rounded-full px-2 py-1 font-semibold ' + status.tone}>{status.label}</span></td>}<td className="p-2 text-xs tabular-nums text-slate-600">{latest ? new Date(latest).toLocaleString('vi-VN') : 'Chưa có vi phạm'}</td></tr>
      })}</tbody></table></div>
      {!data.participants.length && <p className="py-5 text-sm text-slate-500">Chưa có người tham gia.</p>}
      <div className="mt-4 flex flex-wrap items-center justify-between gap-2 text-sm"><span>{data.total} người · Trang {data.page}</span><div className="flex gap-2"><button className="ui-button-secondary" disabled={page <= 1} onClick={() => setPage(p => p - 1)}>Trước</button><button className="ui-button-secondary" disabled={page * data.pageSize >= data.total} onClick={() => setPage(p => p + 1)}>Sau</button></div></div></>}
    {timelineTarget && <EventTimeline key={`${timelineTarget.userId}:${tab}`} contestId={contestId} userId={timelineTarget.userId} name={timelineTarget.name} kind={tab} label={selected.label} onClose={() => setTimelineTarget(null)} />}
  </>
  if (embedded) return <div className="rounded-xl border border-slate-200 bg-white p-4 sm:p-6">{content}</div>
  return <ModalDialog title="Thống kê giám sát" onClose={onClose ?? (() => undefined)} maxWidth="max-w-2xl">{content}</ModalDialog>
}

function EventTimeline({ contestId, userId, name, kind, label, onClose }: { contestId: string; userId: number; name: string; kind: ReportTab; label: string; onClose: () => void }) {
  const [page, setPage] = useState(1)
  const [data, setData] = useState<CameraTimeline | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  useEffect(() => {
    const controller = new AbortController()
    setLoading(true); setError('')
    const path = `/api/teacher/contests/${encodeURIComponent(contestId)}/camera/events?userId=${encodeURIComponent(userId)}&kind=${encodeURIComponent(kind)}&page=${page}`
    void contestRequest<CameraTimeline>(path, { signal: controller.signal })
      .then(result => { if (!controller.signal.aborted) setData(result) })
      .catch(reason => { if (!controller.signal.aborted) setError(reason instanceof Error ? reason.message : 'Không thể tải thời gian sự kiện.') })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [contestId, kind, page, userId])
  return <section className="mt-5 rounded-lg border border-blue-100 bg-blue-50/50 p-4" aria-label={`Thời gian ${label} của ${name}`}>
    <div className="flex items-start justify-between gap-3"><div><h3 className="text-sm font-bold text-slate-900">{label} · {name}</h3><p className="mt-1 text-xs text-slate-500">Thời gian do server ghi nhận, mới nhất trước.</p></div><button type="button" onClick={onClose} className="cursor-pointer text-xs font-semibold text-slate-500 hover:text-slate-900">Đóng</button></div>
    {loading && <p role="status" className="mt-4 text-sm text-slate-500">Đang tải thời gian…</p>}
    {error && <p role="alert" className="mt-4 text-sm text-red-700">{error}</p>}
    {data && !loading && <><ol className="mt-4 grid gap-2 sm:grid-cols-2">{data.occurrences.map((at, index) => <li key={`${at}:${index}`} className="rounded-md border border-slate-200 bg-white px-3 py-2 font-mono text-xs text-slate-700">{new Date(at).toLocaleString('vi-VN')}</li>)}</ol>
      {!data.occurrences.length && <p className="mt-4 text-sm text-slate-500">Không có sự kiện trong thời hạn lưu dữ liệu.</p>}
      <div className="mt-4 flex items-center justify-between gap-2 text-xs text-slate-500"><span>{data.total} sự kiện · Trang {data.page}</span><div className="flex gap-2"><button type="button" className="ui-button-secondary" disabled={page <= 1} onClick={() => setPage(value => value - 1)}>Trước</button><button type="button" className="ui-button-secondary" disabled={page * data.pageSize >= data.total} onClick={() => setPage(value => value + 1)}>Sau</button></div></div></>}
  </section>
}
