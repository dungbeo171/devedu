package com.devedu.learningplatform.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record ProgrammingProblem(
        UUID id,
        String slug,
        String title,
        String summary,
        String description,
        String sampleInput,
        String sampleOutput,
        ProblemTopic topic,
        ProblemDifficulty difficulty,
        Set<CodeLanguage> allowedLanguages,
        Instant createdAt
) {

    public ProgrammingProblem {
        Objects.requireNonNull(id, "Problem id is required");
        slug = requireText(slug, "Problem slug is required");
        title = requireText(title, "Problem title is required");
        summary = requireText(summary, "Problem summary is required");
        description = requireText(description, "Problem description is required");
        sampleInput = sampleInput == null ? "" : sampleInput;
        sampleOutput = sampleOutput == null ? "" : sampleOutput;
        Objects.requireNonNull(topic, "Problem topic is required");
        Objects.requireNonNull(difficulty, "Problem difficulty is required");
        if (allowedLanguages == null || allowedLanguages.isEmpty()) {
            throw new IllegalArgumentException("At least one allowed language is required");
        }
        allowedLanguages = Set.copyOf(allowedLanguages);
        Objects.requireNonNull(createdAt, "Problem created time is required");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
