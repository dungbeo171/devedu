package com.devedu.learningplatform.application.port.in.command;

import com.devedu.learningplatform.domain.model.UserRole;

public record CreateManagedUserCommand(
        UserRole actorRole,
        String name,
        String email,
        String password,
        UserRole role
) {
}
