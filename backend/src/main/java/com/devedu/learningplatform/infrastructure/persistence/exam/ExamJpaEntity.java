package com.devedu.learningplatform.infrastructure.persistence.exam;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "exams")
class ExamJpaEntity {
    @Id private UUID id;
    @Column(nullable = false, unique = true, length = 120) private String slug;
    @Column(nullable = false, length = 180) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String description;
    @Column(name = "teacher_id", nullable = false) private UUID teacherId;
    @Column(name = "scheduled_at", nullable = false) private Instant scheduledAt;
    @Column(name = "duration_minutes", nullable = false) private int durationMinutes;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected ExamJpaEntity() {}
    ExamJpaEntity(UUID id, String slug, String title, String description, UUID teacherId, Instant scheduledAt, int durationMinutes, Instant createdAt) {
        this.id=id; this.slug=slug; this.title=title; this.description=description; this.teacherId=teacherId;
        this.scheduledAt=scheduledAt; this.durationMinutes=durationMinutes; this.createdAt=createdAt;
    }
    UUID getId(){return id;} String getSlug(){return slug;} String getTitle(){return title;} String getDescription(){return description;}
    UUID getTeacherId(){return teacherId;} Instant getScheduledAt(){return scheduledAt;} int getDurationMinutes(){return durationMinutes;} Instant getCreatedAt(){return createdAt;}
}
