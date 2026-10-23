package com.devedu.learningplatform.domain.contest;

import com.devedu.learningplatform.domain.model.ProblemTestCase;
import com.devedu.learningplatform.domain.model.ProgrammingProblem;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** A problem owned by one contest and intentionally absent from the public problem catalog. */
public record ContestProblemDefinition(UUID contestId, ProgrammingProblem problem, List<ProblemTestCase> testCases) {
    public ContestProblemDefinition {
        Objects.requireNonNull(contestId, "Contest id is required");
        Objects.requireNonNull(problem, "Contest problem is required");
        testCases = List.copyOf(Objects.requireNonNull(testCases, "Contest problem test cases are required"));
    }
}
