package com.devedu.learningplatform.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record InterviewQuestion(UUID id, String question, String answer, String explanation,
                                InterviewDifficulty difficulty, InterviewTopic topic, Instant createdAt) {
    public InterviewQuestion {
        Objects.requireNonNull(id, "Interview question id is required");
        question = requireText(question, "Interview question is required");
        answer = requireText(answer, "Interview answer is required");
        explanation = requireText(explanation, "Interview explanation is required");
        Objects.requireNonNull(difficulty, "Interview difficulty is required");
        Objects.requireNonNull(topic, "Interview topic is required");
        Objects.requireNonNull(createdAt, "Interview question created time is required");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }
}
