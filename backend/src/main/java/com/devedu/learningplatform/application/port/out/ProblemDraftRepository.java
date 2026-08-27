package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.domain.model.ProblemDraft;

import java.util.Optional;
import java.util.UUID;

public interface ProblemDraftRepository {

    Optional<ProblemDraft> findByStudentIdAndProblemId(UUID studentId, UUID problemId);

    ProblemDraft save(ProblemDraft draft);
}
