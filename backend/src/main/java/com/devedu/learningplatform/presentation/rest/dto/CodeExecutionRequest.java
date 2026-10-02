package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.CodeLanguage;
import java.util.Map;

public record CodeExecutionRequest(CodeLanguage language, String code, String input, Map<String, String> files) {
}
