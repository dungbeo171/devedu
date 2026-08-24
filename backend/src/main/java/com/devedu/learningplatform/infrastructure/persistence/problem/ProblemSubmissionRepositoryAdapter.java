package com.devedu.learningplatform.infrastructure.persistence.problem;

import com.devedu.learningplatform.application.port.out.ProblemSubmissionRepository;
import com.devedu.learningplatform.domain.model.ProblemSubmission;
import org.springframework.stereotype.Repository;

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
