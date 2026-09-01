package com.devedu.learningplatform.application.port.in;

import com.devedu.learningplatform.application.port.in.command.UpdateProgrammingProblemCommand;
import com.devedu.learningplatform.application.port.in.result.ManagedProgrammingProblem;
import com.devedu.learningplatform.domain.model.ProgrammingProblem;

public interface ManageProgrammingProblemsUseCase {

    ManagedProgrammingProblem getForManagement(String slug);

    ProgrammingProblem update(UpdateProgrammingProblemCommand command);

    void delete(String slug);
}
