package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.ProblemTopic;
import com.devedu.learningplatform.domain.model.ProblemDifficulty;
import com.devedu.learningplatform.domain.model.CodeLanguage;

import java.util.UUID;
import java.util.Set;

public record ProgrammingProblemSummaryResponse(
        UUID id,
        String slug,
        String title,
        String summary,
        ProblemTopic topic,
        ProblemDifficulty difficulty,
        Set<CodeLanguage> allowedLanguages
) {
}
