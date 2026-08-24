package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.Lesson;

import java.util.UUID;

public record LessonSummaryResponse(UUID id, String title, int position, boolean hasVideo) {
    public static LessonSummaryResponse from(Lesson lesson) {
        return new LessonSummaryResponse(lesson.id(), lesson.title(), lesson.position(), lesson.videoUrl() != null);
    }
}
