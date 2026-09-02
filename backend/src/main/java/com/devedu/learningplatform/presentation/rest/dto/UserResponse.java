package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.UserRole;
import com.devedu.learningplatform.domain.model.User;

import java.time.Instant;

public record UserResponse(long id, long publicId, String studentCode, String teacherCode,
                           String name, String email, UserRole role, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.publicId(), user.publicId(), user.studentCode(), user.teacherCode(),
                user.name(), user.email(), user.role(), user.createdAt());
    }
}
