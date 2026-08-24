package com.devedu.learningplatform.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ExamAnswer(UUID id, UUID attemptId, UUID questionId, Integer selectedOptionIndex,
                         String sourceCode, Instant answeredAt) {
    public ExamAnswer {
        Objects.requireNonNull(id, "Answer id is required");
        Objects.requireNonNull(attemptId, "Attempt id is required");
        Objects.requireNonNull(questionId, "Question id is required");
        Objects.requireNonNull(answeredAt, "Answer time is required");
    }
}
