package com.devedu.learningplatform.application.port.in.command;

import com.devedu.learningplatform.domain.model.CodeLanguage;
import java.util.Map;

public record ExecuteCodeCommand(CodeLanguage language, String code, String input, Map<String, String> files) {
    public ExecuteCodeCommand(CodeLanguage language, String code, String input) {
        this(language, code, input, Map.of());
    }
}
