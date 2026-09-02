package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.application.port.in.result.ManagedCourse;
import com.devedu.learningplatform.domain.model.CourseStatus;

import java.time.LocalDate;
import java.util.UUID;

public record ManagedCourseResponse(
        UUID id,
        String code,
        String title,
        String description,
        String teacherName,
        long studentCount,
        LocalDate startDate,
        LocalDate endDate,
        CourseStatus status
) {
    public static ManagedCourseResponse from(ManagedCourse managedCourse) {
        var course = managedCourse.course();
        return new ManagedCourseResponse(course.id(), course.slug().toUpperCase(java.util.Locale.ROOT),
                course.title(), course.description(), managedCourse.teacherName(), managedCourse.studentCount(), course.startDate(),
                course.endDate(), managedCourse.status());
    }
}
