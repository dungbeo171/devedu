package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.ProblemTopic;

import java.util.UUID;

public record ProgrammingProblemSummaryResponse(
        UUID id,
        String slug,
        String title,
        String summary,
        ProblemTopic topic
) {
}

