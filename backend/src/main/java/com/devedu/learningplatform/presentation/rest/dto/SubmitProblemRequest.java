package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.CodeLanguage;

public record SubmitProblemRequest(CodeLanguage language, String sourceCode) {
}

