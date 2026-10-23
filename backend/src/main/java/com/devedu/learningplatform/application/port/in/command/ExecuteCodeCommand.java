package com.devedu.learningplatform.application.port.in.command;

import com.devedu.learningplatform.domain.model.CodeLanguage;
import java.util.Map;
import java.util.UUID;

public record ExecuteCodeCommand(
        CodeLanguage language,
        String code,
        String input,
        Map<String, String> files,
        UUID userId
) {
    public ExecuteCodeCommand(CodeLanguage language, String code, String input, Map<String, String> files) {
        this(language, code, input, files, null);
    }

    public ExecuteCodeCommand(CodeLanguage language, String code, String input) {
        this(language, code, input, Map.of(), null);
    }
}
