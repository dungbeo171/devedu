package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.CourseTopic;

import java.time.Instant;
import java.util.UUID;

public record CourseTopicCreatedResponse(UUID id, UUID courseId, String title, int position, Instant createdAt) {
    public static CourseTopicCreatedResponse from(CourseTopic topic) {
        return new CourseTopicCreatedResponse(topic.id(), topic.courseId(), topic.title(), topic.position(), topic.createdAt());
    }
}
