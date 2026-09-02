package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.application.port.in.result.CourseProblemProgress;
import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ProblemDifficulty;
import com.devedu.learningplatform.domain.model.ProblemTopic;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CourseProblemResponse(UUID id, String slug, String title, String summary,
        ProblemTopic topic, ProblemDifficulty difficulty, Set<CodeLanguage> allowedLanguages,
        Instant assignedAt, boolean solved) {
    public static CourseProblemResponse from(CourseProblemProgress progress) {
        var problem = progress.problem();
        return new CourseProblemResponse(problem.id(), problem.slug(), problem.title(), problem.summary(),
                problem.topic(), problem.difficulty(), problem.allowedLanguages(), progress.assignedAt(), progress.solved());
    }
}
