package com.devedu.learningplatform.infrastructure.persistence.problem;

import com.devedu.learningplatform.domain.model.ProblemTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataProgrammingProblemRepository
        extends JpaRepository<ProgrammingProblemJpaEntity, UUID> {

    List<ProgrammingProblemJpaEntity> findAllByOrderByTitleAsc();

    List<ProgrammingProblemJpaEntity> findAllByTopicOrderByTitleAsc(ProblemTopic topic);

    Optional<ProgrammingProblemJpaEntity> findBySlug(String slug);
}

