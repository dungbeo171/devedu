package com.devedu.learningplatform.infrastructure.persistence.problem;

import com.devedu.learningplatform.application.port.out.ProblemSubmissionRepository;
import com.devedu.learningplatform.domain.model.ProblemSubmission;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;

@Repository
public class ProblemSubmissionRepositoryAdapter implements ProblemSubmissionRepository {

    private final SpringDataProblemSubmissionRepository repository;

    public ProblemSubmissionRepositoryAdapter(SpringDataProblemSubmissionRepository repository) {
        this.repository = repository;
    }

    @Override
    public ProblemSubmission save(ProblemSubmission submission) {
        var entity = new ProblemSubmissionJpaEntity(
                submission.id(),
                submission.problemId(),
                submission.studentId(),
                submission.language(),
                submission.sourceCode(),
                submission.status(),
                submission.diagnostic(),
                submission.passedTests(),
                submission.totalTests(),
                submission.executionTimeMillis(),
                submission.submittedAt()
        );
        return toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public Set<UUID> findAcceptedProblemIdsByStudentId(UUID studentId) {
        return new LinkedHashSet<>(repository.findAcceptedProblemIdsByStudentId(studentId));
    }

    @Override
    public Map<UUID, Set<UUID>> findAcceptedProblemIdsByStudentIds(Set<UUID> studentIds) {
        var result = new LinkedHashMap<UUID, Set<UUID>>();
        studentIds.forEach(studentId -> result.put(studentId, new LinkedHashSet<>()));
        if (studentIds.isEmpty()) return result;
        repository.findAcceptedProblemIdsByStudentIds(studentIds)
                .forEach(pair -> result.get(pair.getStudentId()).add(pair.getProblemId()));
        return result;
    }

    private ProblemSubmission toDomain(ProblemSubmissionJpaEntity entity) {
        return new ProblemSubmission(
                entity.getId(),
                entity.getProblemId(),
                entity.getStudentId(),
                entity.getLanguage(),
                entity.getSourceCode(),
                entity.getStatus(),
                entity.getDiagnostic(),
                entity.getPassedTests(),
                entity.getTotalTests(),
                entity.getExecutionTimeMillis(),
                entity.getSubmittedAt()
        );
    }
}
