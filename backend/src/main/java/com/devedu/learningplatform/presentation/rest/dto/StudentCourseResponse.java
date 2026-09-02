package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.application.port.in.result.StudentCourseSummary;
import com.devedu.learningplatform.domain.model.CourseStatus;

import java.time.LocalDate;
import java.util.UUID;

public record StudentCourseResponse(UUID id, String code, String title, String description,
        String teacherName, LocalDate startDate, LocalDate endDate, CourseStatus status,
        int solvedProblems, int totalProblems, int progressPercent) {
    public static StudentCourseResponse from(StudentCourseSummary summary) {
        var course = summary.course();
        return new StudentCourseResponse(course.id(), course.slug().toUpperCase(java.util.Locale.ROOT),
                course.title(), course.description(), summary.teacherName(), course.startDate(), course.endDate(),
                summary.status(), summary.solvedProblems(), summary.totalProblems(), summary.progressPercent());
    }
}
