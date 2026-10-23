import { createPoseTracker, type CameraEventKind, type HeadPose, type PoseState } from './headPose'

export interface FaceLandmark { x: number; y: number; z: number }
export interface FaceConnection { start: number; end: number; emphasized: boolean }
export interface FaceMeshFrame { landmarks: FaceLandmark[]; connections: FaceConnection[] }
export interface ContestCameraSession { resetObservationWindow: () => void }

/** All video processing stays on this device; the caller only receives state/events. */
export async function openContestCamera(video: HTMLVideoElement, signal: AbortSignal,
  onState: (state: PoseState) => void, onEvent: (kind: CameraEventKind) => void, onError: (message: string) => void,
  onMesh: (frame: FaceMeshFrame | null) => void = () => {}): Promise<ContestCameraSession | undefined> {
  if (!window.isSecureContext || !navigator.mediaDevices?.getUserMedia || !window.Worker || !window.createImageBitmap) {
    throw new Error('Camera cần HTTPS (hoặc localhost) và trình duyệt hỗ trợ Web Worker/ImageBitmap.')
  }
  let stream: MediaStream | null = null
  let worker: Worker | null = null
  let timer: ReturnType<typeof setTimeout> | undefined
  let watchdog: ReturnType<typeof setTimeout> | undefined
  let stopped = false
  let lastFrame = -1
  let connections: FaceConnection[] = []
  const tracker = createPoseTracker()
  const stop = () => {
    stopped = true; clearTimeout(timer); clearTimeout(watchdog)
    worker?.terminate(); stream?.getTracks().forEach(track => track.stop())
    video.srcObject = null
    onMesh(null)
    signal.removeEventListener('abort', stop)
  }
  const fail = () => { if (stopped) return; stop(); onError('Camera bị gián đoạn. Kiểm tra thiết bị và bấm Kết nối lại camera; thời gian làm bài vẫn tiếp tục.') }
  signal.addEventListener('abort', stop, { once: true })
  if (signal.aborted) { stop(); return }
  try {
    stream = await navigator.mediaDevices.getUserMedia({ audio: false, video: { width: { ideal: 640 }, height: { ideal: 480 }, facingMode: 'user' } })
    if (stopped) { stream.getTracks().forEach(track => track.stop()); return }
    stream.getVideoTracks().forEach(track => track.addEventListener('ended', fail, { once: true }))
    video.srcObject = stream
    await video.play()
    if (stopped) return
    worker = new Worker(new URL('./faceLandmarker.worker.ts', import.meta.url))
    // One outstanding frame, max ~5 FPS. CPU inference does not block the code editor.
    const frame = async () => {
      if (stopped) return
      if (document.hidden || video.readyState < 2 || video.currentTime === lastFrame || stream?.getVideoTracks()[0]?.muted) {
        onState('UNKNOWN'); timer = setTimeout(() => void frame(), 200); return
      }
      try {
        lastFrame = video.currentTime
        const bitmap = await createImageBitmap(video)
        if (stopped) { bitmap.close(); return }
        watchdog = setTimeout(fail, 15000)
        worker!.postMessage({ type: 'FRAME', bitmap, at: performance.now() }, [bitmap])
      } catch { fail() }
    }
    watchdog = setTimeout(fail, 45000)
    worker.onerror = fail
    worker.onmessage = (event: MessageEvent<{ type: string; pose: HeadPose | null; faces: number; at: number;
      landmarks?: FaceLandmark[] | null; connections?: FaceConnection[] }>) => {
      if (stopped) return
      clearTimeout(watchdog)
      if (event.data.type === 'ERROR') { fail(); return }
      if (event.data.type === 'READY') {
        connections = event.data.connections ?? []
        onEvent('MONITORING_STARTED')
      }
      if (event.data.type === 'POSE') {
        const result = tracker.sample(event.data.pose, event.data.faces, event.data.at)
        onState(result.state)
        onMesh(event.data.landmarks ? { landmarks: event.data.landmarks, connections } : null)
        if (result.event) onEvent(result.event)
      }
      timer = setTimeout(() => void frame(), 200)
    }
    worker.postMessage({ type: 'INIT', origin: window.location.origin })
    return { resetObservationWindow: () => tracker.resetObservationWindow() }
  } catch (reason) {
    stop()
    if (!signal.aborted) throw new Error(reason instanceof DOMException && reason.name === 'NotAllowedError'
      ? 'Chưa được cấp quyền camera. Hãy cho phép camera trong cài đặt trình duyệt rồi kết nối lại.' : 'Không mở được camera. Kiểm tra camera có đang được ứng dụng khác sử dụng không.')
  }
}
