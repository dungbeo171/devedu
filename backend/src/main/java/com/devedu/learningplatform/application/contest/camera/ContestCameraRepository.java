package com.devedu.learningplatform.application.contest.camera;

import com.devedu.learningplatform.domain.contest.CameraEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ContestCameraRepository {
    void append(CameraEvent event);
    List<ContestCameraUseCase.Summary> summaries(UUID contestId, int offset, int limit, Instant since);
    long count(UUID contestId, UUID userId, CameraEvent.Kind kind, Instant since);
    List<Instant> timeline(UUID contestId, long publicUserId, CameraEvent.Kind kind, int offset, int limit, Instant since);
    long timelineCount(UUID contestId, long publicUserId, CameraEvent.Kind kind, Instant since);
    long participantCount(UUID contestId);
    void deleteBefore(Instant cutoff);
}
