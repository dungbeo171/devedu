package com.devedu.learningplatform.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ProblemTestCase(UUID id, UUID problemId, String input, String expectedOutput,
                              int timeLimitMillis, int position) {
    public ProblemTestCase {
        Objects.requireNonNull(id, "Test case id is required");
        Objects.requireNonNull(problemId, "Problem id is required");
        input = input == null ? "" : input;
        expectedOutput = expectedOutput == null ? "" : expectedOutput;
        if (timeLimitMillis < 100 || timeLimitMillis > 30_000) {
            throw new IllegalArgumentException("Test case time limit must be between 100 and 30000 milliseconds");
        }
        if (position < 1) throw new IllegalArgumentException("Test case position must be positive");
    }
}
