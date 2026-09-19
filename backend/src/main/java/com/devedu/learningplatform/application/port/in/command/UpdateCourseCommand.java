package com.devedu.learningplatform.application.port.in.command;

import com.devedu.learningplatform.domain.model.UserRole;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateCourseCommand(
        UUID actorId,
        UserRole actorRole,
        UUID courseId,
        String slug,
        String title,
        String description,
        LocalDate startDate,
        LocalDate endDate
) {
}
