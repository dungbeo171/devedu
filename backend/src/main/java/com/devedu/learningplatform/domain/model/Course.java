package com.devedu.learningplatform.domain.model;

import java.time.Instant;
import java.time.LocalDate;
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
        LocalDate startDate,
        LocalDate endDate,
        Instant createdAt
) {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    public Course {
        Objects.requireNonNull(id, "Course id is required");
        slug = normalizeSlug(slug);
        title = requireText(title, "Course title is required", 180);
        description = optionalText(description, 20_000);
        Objects.requireNonNull(teacherId, "Course teacher id is required");
        Objects.requireNonNull(startDate, "Course start date is required");
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Course end date must not be before start date");
        }
        Objects.requireNonNull(createdAt, "Course created time is required");
    }

    public Course(UUID id, String slug, String title, String description, UUID teacherId, Instant createdAt) {
        this(id, slug, title, description, teacherId,
                LocalDate.ofInstant(createdAt, java.time.ZoneOffset.UTC), null, createdAt);
    }

    public CourseStatus statusOn(LocalDate date) {
        return endDate != null && endDate.isBefore(date) ? CourseStatus.ENDED : CourseStatus.ACTIVE;
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

    private static String optionalText(String value, int maximumLength) {
        var normalized = value == null ? "" : value.trim();
        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException("Course description must not exceed " + maximumLength + " characters");
        }
        return normalized;
    }
}
