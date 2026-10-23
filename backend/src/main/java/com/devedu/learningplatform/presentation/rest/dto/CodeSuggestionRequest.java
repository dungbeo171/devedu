package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.codeassist.CodeSuggestionLanguage;

public record CodeSuggestionRequest(CodeSuggestionLanguage language, String source, int cursor, String context) {
}
