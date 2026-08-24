package com.devedu.learningplatform.infrastructure.persistence.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "course_topics")
class CourseTopicJpaEntity {
    @Id private UUID id;
    @Column(name = "course_id", nullable = false) private UUID courseId;
    @Column(nullable = false, length = 180) private String title;
    @Column(nullable = false) private int position;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected CourseTopicJpaEntity() {}
    CourseTopicJpaEntity(UUID id, UUID courseId, String title, int position, Instant createdAt) {
        this.id = id; this.courseId = courseId; this.title = title; this.position = position; this.createdAt = createdAt;
    }
    UUID getId() { return id; }
    UUID getCourseId() { return courseId; }
    String getTitle() { return title; }
    int getPosition() { return position; }
    Instant getCreatedAt() { return createdAt; }
}
