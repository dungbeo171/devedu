package com.devedu.learningplatform.domain.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public record Exam(UUID id, String slug, String title, String description, UUID teacherId,
                   Instant scheduledAt, int durationMinutes, Instant createdAt) {
    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    public Exam {
        Objects.requireNonNull(id, "Exam id is required");
        slug = normalizeSlug(slug);
        title = requireText(title, "Exam title is required", 180);
        description = requireText(description, "Exam description is required", 20_000);
        Objects.requireNonNull(teacherId, "Exam teacher id is required");
        Objects.requireNonNull(scheduledAt, "Exam scheduled time is required");
        if (durationMinutes < 1 || durationMinutes > 1440) {
            throw new IllegalArgumentException("Exam duration must be between 1 and 1440 minutes");
        }
        Objects.requireNonNull(createdAt, "Exam created time is required");
    }

    public static String normalizeSlug(String value) {
        if (value == null) throw new IllegalArgumentException("Exam slug is required");
        var normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 120 || !SLUG_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Exam slug must contain lowercase letters, numbers and hyphens");
        }
        return normalized;
    }

    private static String requireText(String value, String message, int maximumLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        var normalized = value.trim();
        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException(message.replace(" is required", " must not exceed " + maximumLength + " characters"));
        }
        return normalized;
    }
}
