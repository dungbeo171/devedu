package com.devedu.learningplatform.application.port.in.result;

import com.devedu.learningplatform.domain.model.SubmissionStatus;

import java.util.List;

public record JudgeResult(SubmissionStatus status, String diagnostic, int passedTests,
                          int totalTests, long executionTimeMillis,
                          List<JudgeTestCaseResult> testCases) {

    public JudgeResult {
        testCases = List.copyOf(testCases);
    }
}
