package com.devedu.learningplatform.infrastructure.persistence.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lessons")
class LessonJpaEntity {
    @Id private UUID id;
    @Column(name = "topic_id", nullable = false) private UUID topicId;
    @Column(nullable = false, length = 180) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(name = "video_url", length = 2048) private String videoUrl;
    @Column(nullable = false) private int position;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected LessonJpaEntity() {}
    LessonJpaEntity(UUID id, UUID topicId, String title, String content, String videoUrl, int position, Instant createdAt) {
        this.id = id; this.topicId = topicId; this.title = title; this.content = content;
        this.videoUrl = videoUrl; this.position = position; this.createdAt = createdAt;
    }
    UUID getId() { return id; }
    UUID getTopicId() { return topicId; }
    String getTitle() { return title; }
    String getContent() { return content; }
    String getVideoUrl() { return videoUrl; }
    int getPosition() { return position; }
    Instant getCreatedAt() { return createdAt; }
}
