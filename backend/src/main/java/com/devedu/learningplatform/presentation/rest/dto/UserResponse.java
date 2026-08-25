package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String name, String email, UserRole role, Instant createdAt) {
}
