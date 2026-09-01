package com.devedu.learningplatform.application.port.in.command;

import com.devedu.learningplatform.domain.model.UserRole;

import java.util.UUID;

public record UpdateManagedUserCommand(
        UUID actorId,
        UserRole actorRole,
        UUID userId,
        String name,
        String email,
        String password,
        UserRole role
) {
}
