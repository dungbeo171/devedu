package com.devedu.learningplatform.infrastructure.persistence.problem;

import com.devedu.learningplatform.application.port.out.ProblemDraftRepository;
import com.devedu.learningplatform.domain.model.ProblemDraft;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ProblemDraftRepositoryAdapter implements ProblemDraftRepository {

    private final SpringDataProblemDraftRepository repository;

    public ProblemDraftRepositoryAdapter(SpringDataProblemDraftRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProblemDraft> findByStudentIdAndProblemId(UUID studentId, UUID problemId) {
        return repository.findByStudentIdAndProblemId(studentId, problemId).map(this::toDomain);
    }

    @Override
    @Transactional
    public ProblemDraft save(ProblemDraft draft) {
        var entity = repository.findByStudentIdAndProblemId(draft.studentId(), draft.problemId())
                .orElseGet(() -> new ProblemDraftJpaEntity(UUID.randomUUID(), draft.problemId(), draft.studentId()));
        entity.update(draft.language(), draft.sourceCode(), draft.input(), draft.updatedAt());
        return toDomain(repository.saveAndFlush(entity));
    }

    private ProblemDraft toDomain(ProblemDraftJpaEntity entity) {
        return new ProblemDraft(
                entity.getProblemId(),
                entity.getStudentId(),
                entity.getLanguage(),
                entity.getSourceCode(),
                entity.getInput(),
                entity.getUpdatedAt()
        );
    }
}
