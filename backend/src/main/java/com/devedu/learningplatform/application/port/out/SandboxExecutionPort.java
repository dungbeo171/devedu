package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.application.port.in.command.JudgeSubmissionCommand;
import com.devedu.learningplatform.application.port.in.result.JudgeResult;

public interface SandboxExecutionPort {
    JudgeResult execute(JudgeSubmissionCommand command);
}
