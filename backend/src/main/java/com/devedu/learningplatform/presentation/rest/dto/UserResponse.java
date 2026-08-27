package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.UserRole;
import com.devedu.learningplatform.domain.model.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String name, String email, UserRole role, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.name(), user.email(), user.role(), user.createdAt());
    }
}
