package com.devedu.learningplatform.application.port.in.command;

import com.devedu.learningplatform.domain.model.CodeLanguage;

public record RunProblemTestsCommand(
        String problemSlug,
        CodeLanguage language,
        String sourceCode
) {
}
