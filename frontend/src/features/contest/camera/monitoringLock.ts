import type { CameraEventKind, PoseState } from './headPose'

export const PRESENCE_LOCK_MILLIS = 10_000
export interface PresenceLock { noFaceActive: boolean; lockUntil: number }
export const emptyPresenceLock = (): PresenceLock => ({ noFaceActive: false, lockUntil: 0 })

export function observePresence(lock: PresenceLock, state: PoseState, event: CameraEventKind | undefined, at: number): PresenceLock {
  if (event === 'NO_FACE') return { noFaceActive: true, lockUntil: Math.max(lock.lockUntil, at + PRESENCE_LOCK_MILLIS) }
  if (state === 'FORWARD' || state === 'LOOKING_AWAY') return { ...lock, noFaceActive: false }
  return lock
}

export function presenceLocked(lock: PresenceLock, at: number) {
  return lock.noFaceActive || at < lock.lockUntil
}

export function presenceLockSeconds(lock: PresenceLock, at: number) {
  return Math.max(0, Math.ceil((lock.lockUntil - at) / 1000))
}
