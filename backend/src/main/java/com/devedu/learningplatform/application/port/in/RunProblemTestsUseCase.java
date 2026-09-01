package com.devedu.learningplatform.application.port.in;

import com.devedu.learningplatform.application.port.in.command.RunProblemTestsCommand;
import com.devedu.learningplatform.application.port.in.result.JudgeResult;

public interface RunProblemTestsUseCase {

    JudgeResult runTests(RunProblemTestsCommand command);
}
