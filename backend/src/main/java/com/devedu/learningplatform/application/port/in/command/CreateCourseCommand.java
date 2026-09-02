package com.devedu.learningplatform.application.port.in.command;

import com.devedu.learningplatform.domain.model.UserRole;

import java.util.UUID;
import java.time.LocalDate;

public record CreateCourseCommand(
        UUID actorId,
        UserRole actorRole,
        String slug,
        String title,
        String description,
        LocalDate startDate,
        LocalDate endDate
) {
    public CreateCourseCommand(UUID actorId, UserRole actorRole, String slug, String title, String description) {
        this(actorId, actorRole, slug, title, description, null, null);
    }
}
