package com.devedu.learningplatform.infrastructure.persistence.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lesson_progress")
class LessonProgressJpaEntity {
    @Id private UUID id;
    @Column(name = "student_id", nullable = false) private UUID studentId;
    @Column(name = "lesson_id", nullable = false) private UUID lessonId;
    @Column(name = "completed_at", nullable = false) private Instant completedAt;

    protected LessonProgressJpaEntity() {}
    LessonProgressJpaEntity(UUID id, UUID studentId, UUID lessonId, Instant completedAt) {
        this.id = id; this.studentId = studentId; this.lessonId = lessonId; this.completedAt = completedAt;
    }
    UUID getId() { return id; }
    UUID getStudentId() { return studentId; }
    UUID getLessonId() { return lessonId; }
    Instant getCompletedAt() { return completedAt; }
}
