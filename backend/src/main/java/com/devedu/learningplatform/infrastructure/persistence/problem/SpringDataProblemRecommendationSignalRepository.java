package com.devedu.learningplatform.infrastructure.persistence.problem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface SpringDataProblemRecommendationSignalRepository
        extends JpaRepository<ProblemRecommendationSignalJpaEntity, ProblemRecommendationSignalKey> {

    List<ProblemRecommendationSignalJpaEntity> findAllByIdUserId(UUID userId);

    @Modifying
    @Query(value = """
            INSERT INTO problem_recommendation_signals
                (user_id, signal_type, signal_value, weight, last_used_at)
            VALUES (:userId, :signalType, :signalValue, :weight, :usedAt)
            ON CONFLICT (user_id, signal_type, signal_value)
            DO UPDATE SET
                weight = LEAST(problem_recommendation_signals.weight + EXCLUDED.weight, 1000000),
                last_used_at = EXCLUDED.last_used_at
            """, nativeQuery = true)
    void increment(
            @Param("userId") UUID userId,
            @Param("signalType") String signalType,
            @Param("signalValue") String signalValue,
            @Param("weight") long weight,
            @Param("usedAt") Instant usedAt
    );
}
