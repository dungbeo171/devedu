package com.devedu.learningplatform.application.port.in;

import com.devedu.learningplatform.domain.model.ProblemDraft;

import java.util.Optional;
import java.util.UUID;

public interface GetProblemDraftUseCase {

    Optional<ProblemDraft> getDraft(UUID studentId, String problemSlug);
}
