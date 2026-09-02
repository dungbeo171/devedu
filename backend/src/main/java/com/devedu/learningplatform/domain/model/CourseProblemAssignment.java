package com.devedu.learningplatform.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CourseProblemAssignment(UUID courseId, UUID problemId, Instant assignedAt) {
    public CourseProblemAssignment {
        Objects.requireNonNull(courseId, "Course id is required");
        Objects.requireNonNull(problemId, "Problem id is required");
        Objects.requireNonNull(assignedAt, "Assigned time is required");
    }
}
