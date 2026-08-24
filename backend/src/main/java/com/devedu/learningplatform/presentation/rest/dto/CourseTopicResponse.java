package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.application.port.in.result.CourseTopicDetails;

import java.util.List;
import java.util.UUID;

public record CourseTopicResponse(UUID id, String title, int position, List<LessonSummaryResponse> lessons) {
    public static CourseTopicResponse from(CourseTopicDetails details) {
        return new CourseTopicResponse(details.topic().id(), details.topic().title(), details.topic().position(),
                details.lessons().stream().map(LessonSummaryResponse::from).toList());
    }
}
