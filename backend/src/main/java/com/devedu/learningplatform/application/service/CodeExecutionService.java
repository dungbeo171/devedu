package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.port.in.ExecuteCodeUseCase;
import com.devedu.learningplatform.application.port.in.command.ExecuteCodeCommand;
import com.devedu.learningplatform.application.port.in.result.CodeExecutionResult;
import com.devedu.learningplatform.application.port.out.CodeExecutionPort;
import com.devedu.learningplatform.domain.model.CodeLanguage;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
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
        var files = command.files() == null ? Map.<String, String>of() : command.files();
        if (!files.isEmpty() && command.language() != CodeLanguage.JAVA) {
            throw new IllegalArgumentException("Additional files are only supported for Java");
        }
        if (files.size() > 19) {
            throw new IllegalArgumentException("A Java project must not exceed 20 files including Main.java");
        }
        var names = new HashSet<String>();
        names.add("main.java");
        long totalLength = command.code().length();
        for (var file : files.entrySet()) {
            var name = file.getKey();
            if (name == null || !name.matches("[A-Za-z_][A-Za-z0-9_]{0,79}\\.java")
                    || name.matches("(?i)(CON|PRN|AUX|NUL|COM[0-9]|LPT[0-9])\\.java")
                    || !names.add(name.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Java file names must be unique class names ending in .java; Main.java is reserved");
            }
            if (file.getValue() == null) {
                throw new IllegalArgumentException("File content must not be null");
            }
            totalLength += file.getValue().length();
        }
        if (totalLength > MAXIMUM_CODE_LENGTH) {
            throw new IllegalArgumentException("Total source code must not exceed 100000 characters");
        }

        return sandbox.executeCode(new ExecuteCodeCommand(
                command.language(),
                command.code(),
                command.input() == null ? "" : command.input(),
                Map.copyOf(files)
        ));
    }
}
