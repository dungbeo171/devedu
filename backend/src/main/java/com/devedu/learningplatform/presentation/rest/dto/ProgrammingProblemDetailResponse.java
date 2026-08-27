package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.ProblemTopic;
import com.devedu.learningplatform.domain.model.ProblemDifficulty;
import com.devedu.learningplatform.domain.model.CodeLanguage;

import java.time.Instant;
import java.util.UUID;
import java.util.Set;

public record ProgrammingProblemDetailResponse(
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
        Instant createdAt
) {
}
