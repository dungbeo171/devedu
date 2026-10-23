package com.devedu.learningplatform.application.port.in.command;

import com.devedu.learningplatform.domain.model.CodeLanguage;
import java.util.UUID;

public record RunProblemTestsCommand(
        String problemSlug,
        CodeLanguage language,
        String sourceCode,
        UUID userId
) {
    public RunProblemTestsCommand(String problemSlug, CodeLanguage language, String sourceCode) {
        this(problemSlug, language, sourceCode, null);
    }
}
