package com.devedu.learningplatform.application.port.in;

import com.devedu.learningplatform.application.port.in.command.JudgeSubmissionCommand;
import com.devedu.learningplatform.application.port.in.result.JudgeResult;

public interface CodeJudgeUseCase {
    JudgeResult judge(JudgeSubmissionCommand command);
}
