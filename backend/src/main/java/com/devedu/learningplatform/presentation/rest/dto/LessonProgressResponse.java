package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.LessonProgress;

import java.time.Instant;
import java.util.UUID;

public record LessonProgressResponse(UUID id, UUID lessonId, Instant completedAt) {
    public static LessonProgressResponse from(LessonProgress progress) {
        return new LessonProgressResponse(progress.id(), progress.lessonId(), progress.completedAt());
    }
}
