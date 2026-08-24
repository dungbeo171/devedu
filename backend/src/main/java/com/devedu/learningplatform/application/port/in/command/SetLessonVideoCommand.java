package com.devedu.learningplatform.application.port.in.command;

import com.devedu.learningplatform.domain.model.UserRole;

import java.util.UUID;

public record SetLessonVideoCommand(
        UUID actorId,
        UserRole actorRole,
        UUID lessonId,
        String videoUrl
) {
}

