package com.devedu.learningplatform.application.port.in.result;

import com.devedu.learningplatform.domain.model.ProblemTestCase;
import com.devedu.learningplatform.domain.model.ProgrammingProblem;

import java.util.List;

public record ManagedProgrammingProblem(
        ProgrammingProblem problem,
        List<ProblemTestCase> testCases
) {
    public ManagedProgrammingProblem {
        testCases = List.copyOf(testCases);
    }
}
