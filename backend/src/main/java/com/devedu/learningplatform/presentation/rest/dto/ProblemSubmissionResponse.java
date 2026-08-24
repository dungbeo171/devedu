package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.SubmissionStatus;

import java.time.Instant;
import java.util.UUID;

public record ProblemSubmissionResponse(
        UUID id,
        UUID problemId,
        CodeLanguage language,
        SubmissionStatus status,
        String diagnostic,
        int passedTests,
        int totalTests,
        long executionTimeMillis,
        Instant submittedAt
) {
}
