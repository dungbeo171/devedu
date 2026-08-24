package com.devedu.learningplatform.application.port.in.command;

import com.devedu.learningplatform.domain.model.UserRole;

import java.util.UUID;

public record CreateCourseCommand(
        UUID actorId,
        UserRole actorRole,
        String slug,
        String title,
        String description
) {
}

