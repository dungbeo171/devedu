package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.application.port.in.result.CodeExecutionResult;
import com.devedu.learningplatform.domain.model.CodeLanguage;

public record CodeExecutionResponse(
        CodeLanguage language,
        CodeExecutionResult.Status status,
        String output
) {
}

