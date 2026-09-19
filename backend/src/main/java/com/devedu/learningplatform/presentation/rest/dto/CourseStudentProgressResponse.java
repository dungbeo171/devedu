package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.application.port.in.result.CourseStudentProgress;

import java.time.Instant;

public record CourseStudentProgressResponse(long id, String studentCode, String name, String email,
                                            Instant joinedAt, int solvedProblems, int totalProblems,
                                            int progressPercent) {
    public static CourseStudentProgressResponse from(CourseStudentProgress progress) {
        var student = progress.student();
        return new CourseStudentProgressResponse(student.publicId(), student.studentCode(),
                progress.displayName() == null ? student.name() : progress.displayName(), student.email(),
                progress.enrolledAt(), progress.solvedProblems(), progress.totalProblems(), progress.progressPercent());
    }
}
