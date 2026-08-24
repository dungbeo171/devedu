package com.devedu.learningplatform.application.port.in.command;

import com.devedu.learningplatform.domain.model.CodeLanguage;

import java.util.UUID;

public record SubmitProblemCommand(
        UUID studentId,
        String problemSlug,
        CodeLanguage language,
        String sourceCode
) {
}

