package com.devedu.learningplatform.infrastructure.persistence.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "course_problem_assignments")
class CourseProblemAssignmentJpaEntity {
    @Id private UUID id;
    @Column(name = "course_id", nullable = false) private UUID courseId;
    @Column(name = "problem_id", nullable = false) private UUID problemId;
    @Column(name = "assigned_at", nullable = false) private Instant assignedAt;
    protected CourseProblemAssignmentJpaEntity() {}
    CourseProblemAssignmentJpaEntity(UUID id, UUID courseId, UUID problemId, Instant assignedAt) {
        this.id = id; this.courseId = courseId; this.problemId = problemId; this.assignedAt = assignedAt;
    }
    UUID getCourseId() { return courseId; }
    UUID getProblemId() { return problemId; }
    Instant getAssignedAt() { return assignedAt; }
}
