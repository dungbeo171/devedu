package com.devedu.learningplatform.application.port.in.result;

import com.devedu.learningplatform.domain.model.ProgrammingProblem;

public record ProgrammingProblemListItem(
        ProgrammingProblem problem,
        double acceptanceRate
) {
}
