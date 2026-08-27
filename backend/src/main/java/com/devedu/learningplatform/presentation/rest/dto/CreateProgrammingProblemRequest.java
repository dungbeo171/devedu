package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ProblemDifficulty;
import com.devedu.learningplatform.domain.model.ProblemTopic;

import java.util.List;
import java.util.Set;

public record CreateProgrammingProblemRequest(
        String slug,
        String title,
        String summary,
        String description,
        String sampleInput,
        String sampleOutput,
        ProblemTopic topic,
        ProblemDifficulty difficulty,
        Set<CodeLanguage> allowedLanguages,
        List<CreateProblemTestCaseRequest> testCases
) {
}
