package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ProblemDifficulty;
import com.devedu.learningplatform.domain.model.ProblemTopic;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record CreateProgrammingProblemRequest(
        String slug,
        String title,
        String summary,
        String description,
        String inputDescription,
        String outputDescription,
        String sampleInput,
        String sampleOutput,
        ProblemTopic topic,
        ProblemDifficulty difficulty,
        Set<CodeLanguage> allowedLanguages,
        Map<CodeLanguage, String> starterCodes,
        List<CreateProblemTestCaseRequest> testCases
) {
}
