package com.devedu.learningplatform.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CourseEnrollment(UUID courseId, UUID studentId, Instant enrolledAt, String displayName) {
    public CourseEnrollment(UUID courseId, UUID studentId, Instant enrolledAt) { this(courseId, studentId, enrolledAt, null); }
    public CourseEnrollment {
        Objects.requireNonNull(courseId, "Course id is required");
        Objects.requireNonNull(studentId, "Student id is required");
        Objects.requireNonNull(enrolledAt, "Enrollment time is required");
        displayName = displayName == null || displayName.isBlank() ? null : displayName.trim();
        if (displayName != null && displayName.length() > 100) throw new IllegalArgumentException("Class display name must not exceed 100 characters");
    }
}
