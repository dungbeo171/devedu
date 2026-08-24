package com.devedu.learningplatform.application.port.in.result;

import com.devedu.learningplatform.domain.model.CodeLanguage;

public record CodeExecutionResult(
        CodeLanguage language,
        Status status,
        String output
) {

    public enum Status {
        SUCCESS,
        COMPILE_ERROR,
        RUNTIME_ERROR,
        TIME_LIMIT
    }
}
