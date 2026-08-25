package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.ProblemTopic;

import java.time.Instant;
import java.util.UUID;

public record ProgrammingProblemDetailResponse(
        UUID id,
        String slug,
        String title,
        String summary,
        String description,
        String sampleInput,
        String sampleOutput,
        ProblemTopic topic,
        Instant createdAt
) {
}
