package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.Course;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CourseResponse(UUID id, String slug, String title, String description,
                             LocalDate startDate, LocalDate endDate, Instant createdAt) {
    public static CourseResponse from(Course course) {
        return new CourseResponse(course.id(), course.slug(), course.title(), course.description(),
                course.startDate(), course.endDate(), course.createdAt());
    }
}
