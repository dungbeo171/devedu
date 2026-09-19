package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.domain.model.ProblemSubmission;

import java.util.Set;
import java.util.Map;
import java.util.UUID;

public interface ProblemSubmissionRepository {

    ProblemSubmission save(ProblemSubmission submission);

    Set<UUID> findAcceptedProblemIdsByStudentId(UUID studentId);

    default Map<UUID, Set<UUID>> findAcceptedProblemIdsByStudentIds(Set<UUID> studentIds) {
        var result = new java.util.LinkedHashMap<UUID, Set<UUID>>();
        studentIds.forEach(studentId -> result.put(studentId, findAcceptedProblemIdsByStudentId(studentId)));
        return result;
    }
}
