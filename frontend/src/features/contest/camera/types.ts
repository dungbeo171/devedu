export interface CameraSummary {
  userId: number; name: string; lookingAwayCount: number; noFaceCount: number; monitoringStarts: number
  tabHiddenCount: number; problemCopiedCount: number; contentPastedCount: number; screenshotAttemptCount: number
  lastLookingAwayAt: string | null; lastNoFaceAt: string | null; lastTabHiddenAt: string | null
  lastProblemCopiedAt: string | null; lastContentPastedAt: string | null; lastScreenshotAttemptAt: string | null
  lastReceivedAt: string | null; lastEvent: string | null; attemptStartedAt: string | null
}
export interface CameraMine { canReview: boolean; canMonitor: boolean; lookingAwayCount: number; retentionDays: number; serverTime: string }
export interface CameraReport { participants: CameraSummary[]; total: number; page: number; pageSize: number; retentionDays: number; serverTime: string }
export interface CameraTimeline {
  userId: number; kind: string; occurrences: string[]; total: number; page: number; pageSize: number; serverTime: string
}
