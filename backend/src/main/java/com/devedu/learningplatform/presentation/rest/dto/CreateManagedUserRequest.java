package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.UserRole;

public record CreateManagedUserRequest(String name, String email, String password, UserRole role) {
}
