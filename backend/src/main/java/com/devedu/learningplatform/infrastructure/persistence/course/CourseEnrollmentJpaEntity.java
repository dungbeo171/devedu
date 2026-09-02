package com.devedu.learningplatform.infrastructure.persistence.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "course_enrollments")
class CourseEnrollmentJpaEntity {
    @Id private UUID id;
    @Column(name = "course_id", nullable = false) private UUID courseId;
    @Column(name = "student_id", nullable = false) private UUID studentId;
    @Column(name = "enrolled_at", nullable = false) private Instant enrolledAt;
    @Column(name = "display_name", length = 100) private String displayName;
    protected CourseEnrollmentJpaEntity() {}
    CourseEnrollmentJpaEntity(UUID id, UUID courseId, UUID studentId, Instant enrolledAt) {
        this.id = id; this.courseId = courseId; this.studentId = studentId; this.enrolledAt = enrolledAt;
    }
    UUID getCourseId() { return courseId; }
    UUID getStudentId() { return studentId; }
    Instant getEnrolledAt() { return enrolledAt; }
    String getDisplayName() { return displayName; }
    void setDisplayName(String displayName) { this.displayName = displayName; }
}
