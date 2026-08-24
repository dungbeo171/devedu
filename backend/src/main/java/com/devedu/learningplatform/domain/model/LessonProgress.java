package com.devedu.learningplatform.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record LessonProgress(
        UUID id,
        UUID studentId,
        UUID lessonId,
        Instant completedAt
) {

    public LessonProgress {
        Objects.requireNonNull(id, "Lesson progress id is required");
        Objects.requireNonNull(studentId, "Student id is required");
        Objects.requireNonNull(lessonId, "Lesson id is required");
        Objects.requireNonNull(completedAt, "Lesson completion time is required");
    }
}

