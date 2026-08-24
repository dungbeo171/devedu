package com.devedu.learningplatform.application.port.in.command;

import com.devedu.learningplatform.domain.model.UserRole;

import java.time.Instant;
import java.util.UUID;

public record CreateExamCommand(UUID actorId, UserRole actorRole, String slug, String title,
                                String description, Instant scheduledAt, int durationMinutes) {}
