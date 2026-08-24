package com.devedu.learningplatform.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Lesson(
        UUID id,
        UUID topicId,
        String title,
        String content,
        String videoUrl,
        int position,
        Instant createdAt
) {

    public Lesson {
        Objects.requireNonNull(id, "Lesson id is required");
        Objects.requireNonNull(topicId, "Course topic id is required");
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Lesson title is required");
        }
        title = title.trim();
        if (title.length() > 180) {
            throw new IllegalArgumentException("Lesson title must not exceed 180 characters");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Lesson content is required");
        }
        content = content.trim();
        if (content.length() > 100_000) {
            throw new IllegalArgumentException("Lesson content must not exceed 100000 characters");
        }
        videoUrl = videoUrl == null || videoUrl.isBlank() ? null : videoUrl.trim();
        if (videoUrl != null && videoUrl.length() > 2048) {
            throw new IllegalArgumentException("Video URL must not exceed 2048 characters");
        }
        if (position < 1) {
            throw new IllegalArgumentException("Lesson position must be positive");
        }
        Objects.requireNonNull(createdAt, "Lesson created time is required");
    }

    public Lesson withVideoUrl(String newVideoUrl) {
        return new Lesson(id, topicId, title, content, newVideoUrl, position, createdAt);
    }
}
