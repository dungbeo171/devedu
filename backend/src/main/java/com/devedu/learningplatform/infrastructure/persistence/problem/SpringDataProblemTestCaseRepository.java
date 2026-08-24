package com.devedu.learningplatform.infrastructure.persistence.problem;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.UUID;

interface SpringDataProblemTestCaseRepository extends JpaRepository<ProblemTestCaseJpaEntity, UUID> {
    List<ProblemTestCaseJpaEntity> findAllByProblemIdOrderByPositionAsc(UUID problemId);
}
