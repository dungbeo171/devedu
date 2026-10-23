package com.devedu.learningplatform.application.contest.camera;

import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.domain.contest.CameraEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ContestCameraUseCase {
    void record(UUID contestId, UUID eventId, CameraEvent.Kind kind, AuthenticatedUser user);
    Mine mine(UUID contestId, AuthenticatedUser user);
    Report report(UUID contestId, int page, AuthenticatedUser user);
    Timeline timeline(UUID contestId, long publicUserId, CameraEvent.Kind kind, int page, AuthenticatedUser user);
    record Summary(long userId, String name, long lookingAwayCount, long noFaceCount,
                   long tabHiddenCount, long problemCopiedCount, long contentPastedCount, long screenshotAttemptCount,
                   Instant lastLookingAwayAt, Instant lastNoFaceAt, Instant lastTabHiddenAt,
                   Instant lastProblemCopiedAt, Instant lastContentPastedAt, Instant lastScreenshotAttemptAt,
                   long monitoringStarts, Instant lastReceivedAt, String lastEvent, Instant attemptStartedAt) {}
    record Mine(boolean canReview, boolean canMonitor, long lookingAwayCount, int retentionDays, Instant serverTime) {}
    record Report(List<Summary> participants, long total, int page, int pageSize, int retentionDays, Instant serverTime) {}
    record Timeline(long userId, CameraEvent.Kind kind, List<Instant> occurrences, long total,
                    int page, int pageSize, Instant serverTime) {}
}
