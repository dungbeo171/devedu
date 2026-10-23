import type { CameraEventKind } from './headPose'
export interface CameraObservation { eventId: string; kind: CameraEventKind }

/** Bounded, session-only retry queue. Never persists observations in browser storage. */
export function createCameraDelivery(send: (event: CameraObservation) => Promise<void>,
  onPending: (pending: number, failed: boolean) => void) {
  const queue: CameraObservation[] = []
  let disposed = false, running = false, failures = 0
  let timer: ReturnType<typeof setTimeout> | undefined
  async function deliver() {
    timer = undefined
    if (disposed || running || !queue.length) return
    running = true
    try {
      await send(queue[0])
      if (disposed) return
      queue.shift(); failures = 0; onPending(queue.length, false)
      if (queue.length) timer = setTimeout(() => void deliver(), 700)
    } catch {
      if (disposed) return
      failures++; onPending(queue.length, true)
      if (failures <= 3) timer = setTimeout(() => void deliver(), failures * 1500)
    } finally { running = false }
  }
  return {
    push(kind: CameraEventKind) {
      if (disposed) return
      if (queue.length >= 30) { onPending(queue.length, true); return }
      queue.push({ eventId: crypto.randomUUID(), kind }); onPending(queue.length, failures > 0)
      if (!timer && !running && failures === 0) timer = setTimeout(() => void deliver(), 700)
    },
    retry() { if (disposed) return; clearTimeout(timer); timer = undefined; failures = 0; void deliver() },
    dispose() { disposed = true; clearTimeout(timer); queue.length = 0 },
  }
}
