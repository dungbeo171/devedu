package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.Lesson;

import java.time.Instant;
import java.util.UUID;

public record LessonResponse(UUID id, UUID topicId, String title, String content, String videoUrl, int position, Instant createdAt) {
    public static LessonResponse from(Lesson lesson) {
        return new LessonResponse(lesson.id(), lesson.topicId(), lesson.title(), lesson.content(), lesson.videoUrl(), lesson.position(), lesson.createdAt());
    }
}
