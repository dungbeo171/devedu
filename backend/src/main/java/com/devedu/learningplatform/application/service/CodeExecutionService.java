package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.port.in.ExecuteCodeUseCase;
import com.devedu.learningplatform.application.port.in.command.ExecuteCodeCommand;
import com.devedu.learningplatform.application.port.in.result.CodeExecutionResult;
import com.devedu.learningplatform.application.port.out.CodeExecutionPort;

import java.util.Objects;

public final class CodeExecutionService implements ExecuteCodeUseCase {
    private static final int MAXIMUM_CODE_LENGTH = 100_000;
    private static final int MAXIMUM_INPUT_LENGTH = 100_000;
    private final CodeExecutionPort sandbox;

    public CodeExecutionService(CodeExecutionPort sandbox) {
        this.sandbox = Objects.requireNonNull(sandbox, "Sandbox is required");
    }

    @Override
    public CodeExecutionResult execute(ExecuteCodeCommand command) {
        Objects.requireNonNull(command, "Execute code command is required");
        Objects.requireNonNull(command.language(), "Language is required");
        if (command.code() == null || command.code().isBlank()) {
            throw new IllegalArgumentException("Code is required");
        }
        if (command.code().length() > MAXIMUM_CODE_LENGTH) {
            throw new IllegalArgumentException("Code must not exceed 100000 characters");
        }
        if (command.input() != null && command.input().length() > MAXIMUM_INPUT_LENGTH) {
            throw new IllegalArgumentException("Input must not exceed 100000 characters");
        }

        return sandbox.executeCode(new ExecuteCodeCommand(
                command.language(),
                command.code(),
                command.input() == null ? "" : command.input()
        ));
    }
}
