package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.User;

public record CourseStudentCandidateResponse(long id, String studentCode, String name, String email) {
    public static CourseStudentCandidateResponse from(User user) {
        return new CourseStudentCandidateResponse(user.publicId(), user.studentCode(), user.name(), user.email());
    }
}
