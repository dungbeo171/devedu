package com.devedu.learningplatform.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CourseTopic(
        UUID id,
        UUID courseId,
        String title,
        int position,
        Instant createdAt
) {

    public CourseTopic {
        Objects.requireNonNull(id, "Course topic id is required");
        Objects.requireNonNull(courseId, "Course id is required");
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Course topic title is required");
        }
        title = title.trim();
        if (title.length() > 180) {
            throw new IllegalArgumentException("Course topic title must not exceed 180 characters");
        }
        if (position < 1) {
            throw new IllegalArgumentException("Course topic position must be positive");
        }
        Objects.requireNonNull(createdAt, "Course topic created time is required");
    }
}
