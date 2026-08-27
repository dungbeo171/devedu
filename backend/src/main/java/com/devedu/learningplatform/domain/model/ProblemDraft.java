package com.devedu.learningplatform.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ProblemDraft(
        UUID problemId,
        UUID studentId,
        CodeLanguage language,
        String sourceCode,
        String input,
        Instant updatedAt
) {
    public ProblemDraft {
        Objects.requireNonNull(problemId, "Problem id is required");
        Objects.requireNonNull(studentId, "Student id is required");
        Objects.requireNonNull(language, "Draft language is required");
        sourceCode = sourceCode == null ? "" : sourceCode;
        input = input == null ? "" : input;
        Objects.requireNonNull(updatedAt, "Draft update time is required");
    }
}
