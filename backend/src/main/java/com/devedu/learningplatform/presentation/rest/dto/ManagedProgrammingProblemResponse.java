package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ProblemDifficulty;
import com.devedu.learningplatform.domain.model.ProblemTopic;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record ManagedProgrammingProblemResponse(
        UUID id,
        String slug,
        String title,
        String summary,
        String description,
        String sampleInput,
        String sampleOutput,
        ProblemTopic topic,
        ProblemDifficulty difficulty,
        Set<CodeLanguage> allowedLanguages,
        Map<CodeLanguage, String> starterCodes,
        Instant createdAt,
        List<CreateProblemTestCaseRequest> testCases
) {
}
