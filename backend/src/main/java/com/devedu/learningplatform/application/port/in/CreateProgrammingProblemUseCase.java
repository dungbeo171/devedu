package com.devedu.learningplatform.application.port.in;

import com.devedu.learningplatform.application.port.in.command.CreateProgrammingProblemCommand;
import com.devedu.learningplatform.domain.model.ProgrammingProblem;

public interface CreateProgrammingProblemUseCase {
    ProgrammingProblem create(CreateProgrammingProblemCommand command);
}
