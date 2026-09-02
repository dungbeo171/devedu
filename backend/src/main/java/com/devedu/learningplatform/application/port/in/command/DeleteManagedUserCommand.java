package com.devedu.learningplatform.application.port.in.command;

import com.devedu.learningplatform.domain.model.UserRole;

import java.util.UUID;

public record DeleteManagedUserCommand(
        UUID actorId,
        UserRole actorRole,
        long userId
) {
}
