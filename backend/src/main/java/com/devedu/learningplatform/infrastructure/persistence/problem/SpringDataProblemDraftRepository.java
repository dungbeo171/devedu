package com.devedu.learningplatform.infrastructure.persistence.problem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataProblemDraftRepository extends JpaRepository<ProblemDraftJpaEntity, UUID> {

    Optional<ProblemDraftJpaEntity> findByStudentIdAndProblemId(UUID studentId, UUID problemId);
}
