package com.devedu.learningplatform.application.port.in.command;

import com.devedu.learningplatform.domain.model.UserRole;

import java.util.UUID;

public record ManageCourseProblemCommand(UUID actorId, UserRole actorRole, UUID courseId, UUID problemId) {}
