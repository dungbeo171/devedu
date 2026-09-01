package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.SubmissionStatus;

import java.util.List;

public record ProblemTestRunResponse(
        SubmissionStatus status,
        String diagnostic,
        int passedTests,
        int totalTests,
        long executionTimeMillis,
        List<ProblemTestCaseResultResponse> testCases
) {
}
