package com.devedu.learningplatform.infrastructure.persistence.problem;

import com.devedu.learningplatform.domain.model.ProblemTopic;
import com.devedu.learningplatform.domain.model.ProblemDifficulty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataProgrammingProblemRepository
        extends JpaRepository<ProgrammingProblemJpaEntity, UUID> {

    @Query("""
            select problem from ProgrammingProblemJpaEntity problem
            where (:topic is null or problem.topic = :topic)
              and (:difficulty is null or problem.difficulty = :difficulty)
              and (:language = '' or concat(',', problem.allowedLanguages, ',') like concat('%,', :language, ',%'))
            order by problem.title asc
            """)
    List<ProgrammingProblemJpaEntity> findAllFiltered(
            @Param("topic") ProblemTopic topic,
            @Param("difficulty") ProblemDifficulty difficulty,
            @Param("language") String language
    );

    Optional<ProgrammingProblemJpaEntity> findBySlug(String slug);
}
