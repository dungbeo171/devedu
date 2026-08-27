package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.CodeLanguage;

public record SaveProblemDraftRequest(CodeLanguage language, String sourceCode, String input) {
}
