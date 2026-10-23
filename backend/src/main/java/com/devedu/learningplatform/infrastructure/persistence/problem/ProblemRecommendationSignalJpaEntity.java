package com.devedu.learningplatform.infrastructure.persistence.problem;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "problem_recommendation_signals")
class ProblemRecommendationSignalJpaEntity {

    @EmbeddedId
    private ProblemRecommendationSignalKey id;

    @Column(nullable = false)
    private long weight;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    protected ProblemRecommendationSignalJpaEntity() {
    }

    ProblemRecommendationSignalKey getId() { return id; }
    long getWeight() { return weight; }
}
