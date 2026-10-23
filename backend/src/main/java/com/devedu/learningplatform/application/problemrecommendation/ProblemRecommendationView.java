package com.devedu.learningplatform.application.problemrecommendation;

import com.devedu.learningplatform.domain.model.ProgrammingProblem;

public record ProblemRecommendationView(
        ProgrammingProblem problem,
        double acceptanceRate
) {
}
