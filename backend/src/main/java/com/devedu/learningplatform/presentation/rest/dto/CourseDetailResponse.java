package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.application.port.in.result.CourseDetails;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CourseDetailResponse(UUID id, String slug, String title, String description, Instant createdAt, List<CourseTopicResponse> topics) {
    public static CourseDetailResponse from(CourseDetails details) {
        var course = details.course();
        return new CourseDetailResponse(course.id(), course.slug(), course.title(), course.description(), course.createdAt(),
                details.topics().stream().map(CourseTopicResponse::from).toList());
    }
}
