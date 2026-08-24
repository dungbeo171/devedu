package com.devedu.learningplatform.application.port.in.command;

import com.devedu.learningplatform.domain.model.UserRole;

import java.util.UUID;

public record CreateLessonCommand(
        UUID actorId,
        UserRole actorRole,
        UUID topicId,
        String title,
        String content,
        int position
) {
}

