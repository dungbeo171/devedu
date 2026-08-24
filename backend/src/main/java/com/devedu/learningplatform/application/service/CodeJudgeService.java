package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.port.in.CodeJudgeUseCase;
import com.devedu.learningplatform.application.port.in.command.JudgeSubmissionCommand;
import com.devedu.learningplatform.application.port.in.result.JudgeResult;
import com.devedu.learningplatform.application.port.out.SandboxExecutionPort;
import java.util.Objects;

public final class CodeJudgeService implements CodeJudgeUseCase {
    private final SandboxExecutionPort sandbox;
    public CodeJudgeService(SandboxExecutionPort sandbox) { this.sandbox = sandbox; }
    @Override public JudgeResult judge(JudgeSubmissionCommand command) {
        Objects.requireNonNull(command, "Judge command is required");
        Objects.requireNonNull(command.submissionId(), "Submission id is required");
        Objects.requireNonNull(command.language(), "Submission language is required");
        if (command.sourceCode() == null || command.sourceCode().isBlank()) throw new IllegalArgumentException("Source code is required");
        if (command.testCases() == null || command.testCases().isEmpty()) throw new IllegalStateException("Problem has no test cases");
        return sandbox.execute(command);
    }
}
