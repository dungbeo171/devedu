package com.devedu.learningplatform.domain.contest;

import java.time.Instant;
import java.util.UUID;

/** Client-reported observations, never evidence of intent or a scoring input. */
public record CameraEvent(UUID eventId, UUID contestId, UUID userId, Kind kind, Instant receivedAt) {
    public enum Kind {
        MONITORING_STARTED,
        CAMERA_ACTIVE,
        LOOKING_AWAY,
        NO_FACE,
        TAB_HIDDEN,
        PROBLEM_COPIED,
        CONTENT_PASTED,
        SCREENSHOT_ATTEMPT,
        CAMERA_STOPPED
    }
    public CameraEvent {
        if (eventId == null || contestId == null || userId == null || kind == null || receivedAt == null) {
            throw new IllegalArgumentException("Camera event information is required");
        }
    }
}
