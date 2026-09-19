package com.devedu.learningplatform.infrastructure.persistence.problem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

interface SpringDataProblemRunStatisticsRepository
        extends JpaRepository<ProblemRunStatisticsJpaEntity, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO problem_run_statistics (problem_id, total_runs, successful_runs)
            VALUES (:problemId, 1, :successfulRuns)
            ON CONFLICT (problem_id) DO UPDATE SET
                total_runs = problem_run_statistics.total_runs + 1,
                successful_runs = problem_run_statistics.successful_runs + EXCLUDED.successful_runs
            """, nativeQuery = true)
    void record(
            @Param("problemId") UUID problemId,
            @Param("successfulRuns") int successfulRuns
    );
}
