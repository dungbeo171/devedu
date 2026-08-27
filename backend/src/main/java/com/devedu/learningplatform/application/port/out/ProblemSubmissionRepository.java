package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.domain.model.ProblemSubmission;

import java.util.Set;
import java.util.UUID;

public interface ProblemSubmissionRepository {

    ProblemSubmission save(ProblemSubmission submission);

    Set<UUID> findAcceptedProblemIdsByStudentId(UUID studentId);
}
