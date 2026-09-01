package com.devedu.learningplatform.application.port.in.result;

import com.devedu.learningplatform.domain.model.SubmissionStatus;

public record JudgeTestCaseResult(
        int position,
        boolean passed,
        SubmissionStatus status
) {
}
