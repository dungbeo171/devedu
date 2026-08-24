package com.devedu.learningplatform.application.security;

import com.devedu.learningplatform.domain.model.UserRole;

import java.util.UUID;

public record AuthenticatedUser(UUID id, String email, UserRole role) {
}

