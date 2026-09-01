package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.SubmissionStatus;

public record ProblemTestCaseResultResponse(
        int position,
        boolean passed,
        SubmissionStatus status
) {
}
