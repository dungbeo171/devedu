package com.devedu.learningplatform.infrastructure.persistence.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "courses")
class CourseJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CourseJpaEntity() {
    }

    CourseJpaEntity(UUID id, String slug, String title, String description, UUID teacherId,
                    LocalDate startDate, LocalDate endDate, Instant createdAt) {
        this.id = id;
        this.slug = slug;
        this.title = title;
        this.description = description;
        this.teacherId = teacherId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
    }

    UUID getId() { return id; }
    String getSlug() { return slug; }
    String getTitle() { return title; }
    String getDescription() { return description; }
    UUID getTeacherId() { return teacherId; }
    LocalDate getStartDate() { return startDate; }
    LocalDate getEndDate() { return endDate; }
    Instant getCreatedAt() { return createdAt; }
}
