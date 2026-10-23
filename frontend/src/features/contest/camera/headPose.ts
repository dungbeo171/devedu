export type CameraEventKind = 'MONITORING_STARTED' | 'CAMERA_ACTIVE' | 'LOOKING_AWAY' | 'NO_FACE'
  | 'TAB_HIDDEN' | 'PROBLEM_COPIED' | 'CONTENT_PASTED' | 'SCREENSHOT_ATTEMPT' | 'CAMERA_STOPPED'
export type PoseState = 'CALIBRATING' | 'FORWARD' | 'LOOKING_AWAY' | 'NO_FACE' | 'UNKNOWN'
export interface HeadPose { yaw: number; pitch: number }

// MediaPipe's column-major facial transform: direction of the head's local Z axis.
export function poseFromMatrix(matrix: number[]): HeadPose | null {
  if (matrix.length !== 16 || matrix.some(v => !Number.isFinite(v))) return null
  const [x, y, z] = [matrix[8], matrix[9], matrix[10]]
  if (Math.hypot(x, y, z) < 0.1) return null
  // Depending on the camera/model build, the neutral local Z axis can point
  // towards either +Z or -Z. Both are a frontal pose. Normalising its sign
  // prevents a neutral -Z matrix from looking like a 180 degree head turn and
  // leaving calibration stuck forever.
  return { yaw: Math.atan2(x, Math.abs(z)) * 180 / Math.PI, pitch: Math.atan2(-y, Math.hypot(x, z)) * 180 / Math.PI }
}
const angleDifference = (a: number, b: number) => Math.abs(((a - b + 540) % 360) - 180)
// Face Landmarker can take over one second per frame on lower-powered devices.
// A gap shorter than this still represents a continuously running foreground
// camera. Longer gaps (for example after hiding the tab) reset observation time.
const MAX_CONTINUOUS_SAMPLE_GAP_MS = 3000

/** Conservative head-pose heuristic, NOT eye tracking, attention or cheating detection. */
export function createPoseTracker() {
  let baseline: HeadPose | null = null
  let calibration: HeadPose[] = []
  let previous = -Infinity
  let noFaceSince: number | null = null
  let awayLatched = false
  let missingLatched = false
  return {
    resetObservationWindow() {
      previous = -Infinity
      noFaceSince = null
      awayLatched = missingLatched = false
    },
    sample(pose: HeadPose | null, faces: number, at: number): { state: PoseState; event?: CameraEventKind } {
      if (at - previous > MAX_CONTINUOUS_SAMPLE_GAP_MS || at <= previous) {
        noFaceSince = null
        calibration = []
      }
      previous = at
      if (faces !== 1 || !pose) {
        calibration = []
        if (faces !== 0) { noFaceSince = null; return { state: 'UNKNOWN' } }
        noFaceSince ??= at
        if (!missingLatched && at - noFaceSince >= 2000) {
          missingLatched = true
          return { state: 'NO_FACE', event: 'NO_FACE' }
        }
        return { state: 'NO_FACE' }
      }
      noFaceSince = null
      if (!baseline) {
        if (Math.abs(pose.yaw) > 20 || Math.abs(pose.pitch) > 20 || calibration.some(p => angleDifference(p.yaw, pose.yaw) > 8 || angleDifference(p.pitch, pose.pitch) > 8)) {
          calibration = []; return { state: 'CALIBRATING' }
        }
        calibration.push(pose)
        if (calibration.length >= 20) {
          baseline = { yaw: calibration.reduce((sum, p) => sum + p.yaw, 0) / calibration.length,
            pitch: calibration.reduce((sum, p) => sum + p.pitch, 0) / calibration.length }
        }
        return { state: 'CALIBRATING' }
      }
      const yaw = angleDifference(pose.yaw, baseline.yaw), pitch = angleDifference(pose.pitch, baseline.pitch)
      if (yaw > 30 || pitch > 25) {
        if (!awayLatched) {
          awayLatched = true
          return { state: 'LOOKING_AWAY', event: 'LOOKING_AWAY' }
        }
        return { state: 'LOOKING_AWAY' }
      }
      // Hysteresis: only a clearly frontal frame rearms the next episode.
      if (yaw < 20 && pitch < 15) {
        awayLatched = false
        missingLatched = false
      }
      return { state: 'FORWARD' }
    },
  }
}
