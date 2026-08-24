package com.devedu.learningplatform.application.port.in.result;

import com.devedu.learningplatform.domain.model.SubmissionStatus;

public record JudgeResult(SubmissionStatus status, String diagnostic, int passedTests,
                          int totalTests, long executionTimeMillis) {}
