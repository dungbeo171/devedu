package com.devedu.learningplatform.application.port.in;

import com.devedu.learningplatform.application.port.in.command.SubmitProblemCommand;
import com.devedu.learningplatform.domain.model.ProblemSubmission;

public interface SubmitProgrammingProblemUseCase {

    ProblemSubmission submit(SubmitProblemCommand command);
}

