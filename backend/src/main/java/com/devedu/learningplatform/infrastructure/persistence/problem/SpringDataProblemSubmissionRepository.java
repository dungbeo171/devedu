package com.devedu.learningplatform.infrastructure.persistence.problem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataProblemSubmissionRepository
        extends JpaRepository<ProblemSubmissionJpaEntity, UUID> {
}

