package com.devedu.learningplatform.application.port.in;

import com.devedu.learningplatform.domain.model.ProgrammingProblem;

public interface GetProgrammingProblemUseCase {

    ProgrammingProblem getBySlug(String slug);
}

