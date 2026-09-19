package com.devedu.learningplatform.infrastructure.persistence.problem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "problem_run_statistics")
class ProblemRunStatisticsJpaEntity {

    @Id
    @Column(name = "problem_id")
    private UUID problemId;

    @Column(name = "total_runs", nullable = false)
    private long totalRuns;

    @Column(name = "successful_runs", nullable = false)
    private long successfulRuns;

    protected ProblemRunStatisticsJpaEntity() {
    }

    UUID getProblemId() {
        return problemId;
    }

    long getTotalRuns() {
        return totalRuns;
    }

    long getSuccessfulRuns() {
        return successfulRuns;
    }
}
