package com.devedu.learningplatform.domain.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public record Course(
        UUID id,
        String slug,
        String title,
        String description,
        UUID teacherId,
        Instant createdAt
) {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    public Course {
        Objects.requireNonNull(id, "Course id is required");
        slug = normalizeSlug(slug);
        title = requireText(title, "Course title is required", 180);
        description = requireText(description, "Course description is required", 20_000);
        Objects.requireNonNull(teacherId, "Course teacher id is required");
        Objects.requireNonNull(createdAt, "Course created time is required");
    }

    public static String normalizeSlug(String slug) {
        if (slug == null) {
            throw new IllegalArgumentException("Course slug is required");
        }
        var normalized = slug.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 120 || !SLUG_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Course slug must contain lowercase letters, numbers and hyphens");
        }
        return normalized;
    }

    private static String requireText(String value, String message, int maximumLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        var normalized = value.trim();
        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException(message.replace(" is required", " must not exceed " + maximumLength + " characters"));
        }
        return normalized;
    }
}
