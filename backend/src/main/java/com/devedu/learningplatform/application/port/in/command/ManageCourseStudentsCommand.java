package com.devedu.learningplatform.application.port.in.command;

import com.devedu.learningplatform.domain.model.UserRole;

import java.util.List;
import java.util.UUID;

public record ManageCourseStudentsCommand(
        UUID actorId,
        UserRole actorRole,
        UUID courseId,
        List<Long> studentIds
) {}
