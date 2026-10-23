import { useEffect, useRef, useState } from 'react'
import { getStoredAccessToken, getStoredUser } from '../../auth/api/authApi'
import { contestRequest } from '../api/contestApi'
import { useContestResource } from '../hooks/useContestResource'
import { openContestCamera, type ContestCameraSession } from './cameraCapture'
import { createCameraDelivery } from './eventDelivery'
import { drawFaceMesh } from './faceMeshOverlay'
import { CameraReportDialog } from './CameraReportDialog'
import { IconShield } from '../../../shared/components/Icons'
import type { CameraEventKind, PoseState } from './headPose'
import { emptyPresenceLock, observePresence, presenceLocked, presenceLockSeconds } from './monitoringLock'
import type { CameraMine } from './types'

type Props = {
  contestId: string; userId: number; live: boolean; registered: boolean
  monitoringEnabled: boolean; attemptStarted: boolean; attemptEnded: boolean
  onReadyChange: (ready: boolean) => void
}

const CONTEST_PROBLEM_COPY_EVENT = 'devedu:contest-problem-copy'

export function ContestCameraPanel({ contestId, userId, live, registered, monitoringEnabled,
  attemptStarted, attemptEnded, onReadyChange }: Props) {
  const path = '/api/contests/' + encodeURIComponent(contestId) + '/camera'
  const consentKey = 'contest-camera-consent:v2:' + userId + ':' + contestId
  const { data, error, reload } = useContestResource<CameraMine>(path + '/me', 10000)
  const [consent, setConsent] = useState(() => {
    try { return sessionStorage.getItem(consentKey) === 'yes' } catch { return false }
  })
  const [ready, setReady] = useState(false)
  const [preview, setPreview] = useState(false)
  const [retry, setRetry] = useState(0)
  const [state, setState] = useState<PoseState | 'STARTING' | 'OFF'>('OFF')
  const [cameraError, setCameraError] = useState('')
  const [delivery, setDelivery] = useState({ pending: 0, failed: false })
  const [lookingAwayCount, setLookingAwayCount] = useState(0)
  const [presenceLock, setPresenceLock] = useState(emptyPresenceLock)
  const [lockNow, setLockNow] = useState(Date.now)
  const [report, setReport] = useState(() => new URLSearchParams(window.location.search).get('monitoring') === '1')
  const video = useRef<HTMLVideoElement>(null)
  const overlay = useRef<HTMLCanvasElement>(null)
  const cameraSession = useRef<ContestCameraSession | null>(null)
  const sender = useRef<ReturnType<typeof createCameraDelivery> | null>(null)
  const eligible = monitoringEnabled && live && registered && !attemptEnded
  const allowed = eligible && Boolean(data?.canMonitor)
  const recording = eligible && attemptStarted
  const recordingRef = useRef(recording)
  recordingRef.current = recording

  useEffect(() => {
    setPresenceLock(emptyPresenceLock())
    setLockNow(Date.now())
    if (recording) cameraSession.current?.resetObservationWindow()
  }, [recording])

  useEffect(() => {
    if (!recording || !presenceLocked(presenceLock, Date.now())) return
    const timer = window.setInterval(() => setLockNow(Date.now()), 250)
    return () => window.clearInterval(timer)
  }, [presenceLock, recording])

  useEffect(() => {
    setLookingAwayCount(current => Math.max(current, data?.lookingAwayCount ?? 0))
  }, [data?.lookingAwayCount])

  useEffect(() => {
    onReadyChange(ready && allowed)
    return () => onReadyChange(false)
  }, [ready, allowed, onReadyChange])

  // No queue exists outside the personal attempt; preparation never counts observations.
  useEffect(() => {
    if (!recording) return
    const token = getStoredAccessToken()
    const controller = new AbortController()
    const queue = createCameraDelivery(async observation => {
      if (!token || getStoredAccessToken() !== token || getStoredUser()?.publicId !== userId) throw new Error('Session changed')
      await contestRequest<void>(path + '/events', { method: 'POST', body: JSON.stringify(observation),
        headers: { Authorization: 'Bearer ' + token }, signal: AbortSignal.any([controller.signal, AbortSignal.timeout(10000)]) })
    }, (pending, failed) => setDelivery({ pending, failed }))
    sender.current = queue
    window.addEventListener('online', queue.retry)
    return () => { controller.abort(); queue.dispose(); sender.current = null; setDelivery({ pending: 0, failed: false }); window.removeEventListener('online', queue.retry) }
  }, [path, userId, recording])

  useEffect(() => {
    if (!consent || !allowed || !video.current) { setReady(false); setState('OFF'); return }
    const controller = new AbortController()
    setReady(false); setState('STARTING'); setCameraError('')
    const emit = (kind: CameraEventKind) => {
      if (kind === 'MONITORING_STARTED') setReady(true)
      else if (recordingRef.current) {
        if (kind === 'LOOKING_AWAY') setLookingAwayCount(count => count + 1)
        if (kind === 'NO_FACE') {
          const now = Date.now()
          setPresenceLock(current => observePresence(current, 'NO_FACE', kind, now))
          setLockNow(now)
        }
        sender.current?.push(kind)
      }
    }
    void openContestCamera(video.current, controller.signal, nextState => {
      setState(nextState)
      if (recordingRef.current) setPresenceLock(current => observePresence(current, nextState, undefined, Date.now()))
    }, emit, message => {
      if (recordingRef.current) sender.current?.push('CAMERA_STOPPED')
      setCameraError(message); setReady(false); setState('OFF')
    }, frame => {
      if (!overlay.current || !video.current) return
      drawFaceMesh(overlay.current, frame, video.current.videoWidth || 640, video.current.videoHeight || 480)
    }).then(session => { cameraSession.current = session ?? null }).catch(reason => {
      if (!controller.signal.aborted) {
        setCameraError(reason instanceof Error ? reason.message : 'Không mở được camera.')
        setReady(false); setState('OFF')
      }
    })
    // Preview, problem navigation and starting the clock do not reopen the stream.
    return () => { cameraSession.current = null; controller.abort() }
  }, [consent, allowed, retry])

  useEffect(() => {
    if (!ready || !recording) return
    sender.current?.push('MONITORING_STARTED')
    // Keep a lightweight liveness signal for the reviewer report. This does
    // not contain images, landmarks or pose measurements.
    const heartbeat = window.setInterval(() => sender.current?.push('CAMERA_ACTIVE'), 10000)
    return () => window.clearInterval(heartbeat)
  }, [ready, recording])

  useEffect(() => {
    if (!recording) return
    let lastScreenshotSignal = -Infinity
    const recordTabSwitch = () => {
      if (document.hidden) sender.current?.push('TAB_HIDDEN')
    }
    const recordScreenshotKey = (event: KeyboardEvent) => {
      const now = performance.now()
      if (!event.repeat && (event.key === 'PrintScreen' || event.code === 'PrintScreen') && now - lastScreenshotSignal > 1000) {
        lastScreenshotSignal = now
        sender.current?.push('SCREENSHOT_ATTEMPT')
      }
    }
    const recordProblemCopy = (event: Event) => {
      const detail = (event as CustomEvent<{ contestId?: string }>).detail
      if (detail?.contestId === contestId) sender.current?.push('PROBLEM_COPIED')
    }
    const recordPaste = () => sender.current?.push('CONTENT_PASTED')
    document.addEventListener('visibilitychange', recordTabSwitch)
    window.addEventListener('keydown', recordScreenshotKey)
    window.addEventListener('keyup', recordScreenshotKey)
    window.addEventListener(CONTEST_PROBLEM_COPY_EVENT, recordProblemCopy)
    document.addEventListener('paste', recordPaste)
    return () => {
      document.removeEventListener('visibilitychange', recordTabSwitch)
      window.removeEventListener('keydown', recordScreenshotKey)
      window.removeEventListener('keyup', recordScreenshotKey)
      window.removeEventListener(CONTEST_PROBLEM_COPY_EVENT, recordProblemCopy)
      document.removeEventListener('paste', recordPaste)
    }
  }, [contestId, recording])

  function enable() {
    try { sessionStorage.setItem(consentKey, 'yes') } catch { /* Current view works without storage. */ }
    setConsent(true)
  }
  function closeReport() {
    setReport(false)
    const url = new URL(window.location.href)
    if (url.searchParams.has('monitoring')) {
      url.searchParams.delete('monitoring')
      window.history.replaceState({}, '', url)
    }
  }
  if (!monitoringEnabled && !data?.canReview) return null
  const lockedForPresence = recording && presenceLocked(presenceLock, lockNow)
  const lockedForCamera = recording && !ready
  const workspaceLocked = lockedForPresence || lockedForCamera
  const remainingSeconds = presenceLockSeconds(presenceLock, lockNow)
  return <><section aria-label="Camera cuộc thi" className="mb-4 rounded-lg border border-slate-200 bg-white p-3 text-sm">
    <div className="flex flex-wrap items-center justify-between gap-2"><h2 className="font-semibold">Giám sát cuộc thi</h2>
      {data?.canReview && <button type="button" className="ui-button-secondary" onClick={() => setReport(true)}>Thống kê giám sát</button>}</div>
    {monitoringEnabled && <>
      <div className="mt-3 flex flex-wrap items-center gap-3">
        {allowed && !consent && <button type="button" className="ui-button-primary" onClick={enable}>Đồng ý và cho phép camera</button>}
        {ready && <button type="button" className="ui-button-secondary" aria-pressed={preview} onClick={() => setPreview(value => !value)}>{preview ? 'Ẩn hình camera' : 'Hiện hình camera'}</button>}
        {allowed && consent && cameraError && <button type="button" className="ui-button-secondary" onClick={() => setRetry(value => value + 1)}>Kết nối lại camera</button>}
        {attemptStarted && <span className="inline-flex min-h-9 items-center gap-2 rounded-md bg-blue-50 px-3 text-xs font-semibold text-blue-700"><span className="h-2 w-2 rounded-full bg-blue-500" />Số lần quay ngang: <strong className="tabular-nums">{lookingAwayCount}</strong></span>}
        <span role="status" className="text-xs text-slate-600">{!live ? 'Camera không hoạt động ngoài thời gian cuộc thi.' : !registered ? 'Đăng ký để chuẩn bị camera.' : attemptEnded ? 'Đã hết thời gian làm bài. Camera đã dừng.'
          : !consent ? 'Cần đồng ý và cấp quyền camera trước khi bắt đầu.' : cameraError ? 'Camera bị gián đoạn.' : state === 'STARTING' ? 'Đang mở camera / tải mô hình…'
            : state === 'CALIBRATING' ? 'Nhìn thẳng camera và giữ yên khoảng 4 giây để hiệu chỉnh.' : ready ? 'Camera đang bật' : 'Đang kiểm tra quyền camera…'}</span>
      </div>
      {/* Keep the same playing video element mounted when its preview is hidden. */}
      <div className={preview && ready ? 'relative mt-3 aspect-[4/3] w-72 max-w-full overflow-hidden rounded-lg border border-emerald-400/40 bg-slate-950 shadow-[0_8px_24px_rgba(15,23,42,.14)]' : 'sr-only'}>
        <video ref={video} muted playsInline aria-label="Hình camera chỉ hiển thị trên thiết bị này" aria-hidden={!preview || !ready}
          className="h-full w-full -scale-x-100 object-cover" />
        <canvas ref={overlay} aria-hidden="true" className="pointer-events-none absolute inset-0 h-full w-full -scale-x-100" />
        <div className="pointer-events-none absolute left-2 top-2 rounded bg-slate-950/70 px-2 py-1 text-[10px] font-semibold text-emerald-300 backdrop-blur-sm">
          {state === 'LOOKING_AWAY' ? 'Đang quay ngang' : state === 'FORWARD' ? 'Đang nhìn thẳng' : state === 'CALIBRATING' ? 'Đang hiệu chỉnh' : 'Đang nhận diện'}
        </div>
      </div>
      {cameraError && eligible && <p role="alert" className="mt-2 text-xs text-red-700">{cameraError}</p>}
    </>}
    {error && <p role="alert" className="mt-2 text-xs text-red-700">{error} <button type="button" className="underline" onClick={() => void reload()}>Thử lại</button></p>}
    {recording && delivery.pending > 0 && <p role="status" className={'fixed bottom-4 right-4 z-[90] max-w-[calc(100vw-2rem)] rounded-lg border px-3 py-2 text-xs font-medium shadow-lg ' + (delivery.failed ? 'border-red-200 bg-red-50 text-red-700' : 'border-blue-200 bg-white text-blue-700')}>{delivery.failed ? 'Chưa đồng bộ dữ liệu camera. Rời trang có thể mất dữ liệu chưa gửi.' : 'Đang đồng bộ dữ liệu camera…'} {delivery.failed && <button type="button" className="cursor-pointer font-semibold underline" onClick={() => sender.current?.retry()}>Gửi lại</button>}</p>}
    {report && data?.canReview && <CameraReportDialog contestId={contestId} onClose={closeReport} />}
  </section>
    {workspaceLocked && <div role="alertdialog" aria-modal="true" aria-labelledby="camera-lock-title" className="fixed inset-0 z-[100] grid place-items-center bg-slate-950/45 p-4 backdrop-blur-md">
      <div className="w-full max-w-sm rounded-2xl border border-white/20 bg-white p-6 text-center shadow-2xl">
        <span className="mx-auto grid h-14 w-14 place-items-center rounded-full bg-blue-50 text-blue-700"><IconShield className="h-7 w-7" /></span>
        <h2 id="camera-lock-title" className="mt-4 text-lg font-bold text-slate-950">Bài thi đang bị khóa</h2>
        <p className="mt-2 text-sm leading-6 text-slate-600">{lockedForCamera
          ? 'Camera phải được bật và kết nối ổn định trong suốt thời gian làm bài.'
          : presenceLock.noFaceActive
            ? `Không tìm thấy khuôn mặt trong khung hình. Hãy quay lại camera${remainingSeconds ? ` và chờ ${remainingSeconds} giây` : ''}.`
            : `Đã nhận lại khuôn mặt. Vui lòng chờ ${remainingSeconds} giây để tiếp tục làm bài.`}</p>
        {lockedForCamera && cameraError && <button type="button" className="ui-button-primary mt-5" onClick={() => setRetry(value => value + 1)}>Kết nối lại camera</button>}
        {!lockedForCamera && <div className="mt-5 font-mono text-3xl font-bold tabular-nums text-blue-700">{String(remainingSeconds).padStart(2, '0')}s</div>}
      </div>
    </div>}
  </>
}
