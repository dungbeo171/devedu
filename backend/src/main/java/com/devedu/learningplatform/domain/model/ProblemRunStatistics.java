package com.devedu.learningplatform.domain.model;

import java.util.UUID;

public record ProblemRunStatistics(
        UUID problemId,
        long totalRuns,
        long successfulRuns
) {
    public ProblemRunStatistics {
        if (problemId == null) throw new IllegalArgumentException("Problem id is required");
        if (totalRuns < 0 || successfulRuns < 0 || successfulRuns > totalRuns) {
            throw new IllegalArgumentException("Invalid problem run statistics");
        }
    }

    public double acceptanceRate() {
        return totalRuns == 0 ? 0.0 : successfulRuns * 100.0 / totalRuns;
    }
}
