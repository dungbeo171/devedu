package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.CodeLanguage;

public record CodeExecutionRequest(CodeLanguage language, String code, String input) {
}

