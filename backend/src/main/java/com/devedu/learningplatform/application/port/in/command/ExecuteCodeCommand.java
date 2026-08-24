package com.devedu.learningplatform.application.port.in.command;

import com.devedu.learningplatform.domain.model.CodeLanguage;

public record ExecuteCodeCommand(CodeLanguage language, String code, String input) {
}

