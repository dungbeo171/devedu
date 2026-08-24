package com.devedu.learningplatform.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ProgrammingProblem(
        UUID id,
        String slug,
        String title,
        String summary,
        String description,
        ProblemTopic topic,
        Instant createdAt
) {

    public ProgrammingProblem {
        Objects.requireNonNull(id, "Problem id is required");
        slug = requireText(slug, "Problem slug is required");
        title = requireText(title, "Problem title is required");
        summary = requireText(summary, "Problem summary is required");
        description = requireText(description, "Problem description is required");
        Objects.requireNonNull(topic, "Problem topic is required");
        Objects.requireNonNull(createdAt, "Problem created time is required");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}

