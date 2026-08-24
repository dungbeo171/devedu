package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.application.port.in.command.ExecuteCodeCommand;
import com.devedu.learningplatform.application.port.in.result.CodeExecutionResult;

public interface CodeExecutionPort {
    CodeExecutionResult executeCode(ExecuteCodeCommand command);
}
