package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.application.port.in.result.EnrolledCourseStudent;
import java.time.Instant;

public record CourseStudentResponse(long id, String studentCode, String name, String email,
                                    Instant joinedAt, String status) {
    public static CourseStudentResponse from(EnrolledCourseStudent enrollment) {
        var user = enrollment.user();
        return new CourseStudentResponse(user.publicId(), user.studentCode(),
                enrollment.displayName() == null ? user.name() : enrollment.displayName(), user.email(),
                enrollment.enrolledAt(), "ACTIVE");
    }
}
