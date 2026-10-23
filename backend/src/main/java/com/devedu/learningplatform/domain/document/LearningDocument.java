package com.devedu.learningplatform.domain.document;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record LearningDocument(
        UUID id,
        String slug,
        String title,
        String summary,
        String content,
        List<String> tags,
        int readingMinutes,
        int priority,
        Instant createdAt
) {
    public LearningDocument {
        Objects.requireNonNull(id);
        Objects.requireNonNull(slug);
        Objects.requireNonNull(title);
        Objects.requireNonNull(summary);
        Objects.requireNonNull(content);
        Objects.requireNonNull(tags);
        Objects.requireNonNull(createdAt);
        tags = List.copyOf(tags);
        if (slug.isBlank() || title.isBlank() || content.isBlank() || readingMinutes < 1) {
            throw new IllegalArgumentException("Tài liệu không hợp lệ");
        }
    }
}
