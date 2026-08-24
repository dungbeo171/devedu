package com.devedu.learningplatform.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ProblemSubmission(
        UUID id,
        UUID problemId,
        UUID studentId,
        CodeLanguage language,
        String sourceCode,
        SubmissionStatus status,
        String diagnostic,
        int passedTests,
        int totalTests,
        long executionTimeMillis,
        Instant submittedAt
) {

    public ProblemSubmission {
        Objects.requireNonNull(id, "Submission id is required");
        Objects.requireNonNull(problemId, "Problem id is required");
        Objects.requireNonNull(studentId, "Student id is required");
        Objects.requireNonNull(language, "Submission language is required");
        if (sourceCode == null || sourceCode.isBlank()) {
            throw new IllegalArgumentException("Source code is required");
        }
        Objects.requireNonNull(status, "Submission status is required");
        diagnostic = diagnostic == null ? "" : diagnostic;
        if (passedTests < 0 || totalTests < passedTests || executionTimeMillis < 0) {
            throw new IllegalArgumentException("Submission result is invalid");
        }
        Objects.requireNonNull(submittedAt, "Submission time is required");
    }
}
