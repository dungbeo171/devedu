package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ProblemDraft;

import java.time.Instant;
import java.util.UUID;

public record ProblemDraftResponse(
        UUID problemId,
        CodeLanguage language,
        String sourceCode,
        String input,
        Instant updatedAt
) {
    public static ProblemDraftResponse from(ProblemDraft draft) {
        return new ProblemDraftResponse(
                draft.problemId(),
                draft.language(),
                draft.sourceCode(),
                draft.input(),
                draft.updatedAt()
        );
    }
}
