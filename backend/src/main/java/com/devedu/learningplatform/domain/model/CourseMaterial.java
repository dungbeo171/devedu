package com.devedu.learningplatform.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CourseMaterial(UUID id, UUID courseId, String title, String originalFileName,
                             String storageKey, String contentType, long sizeBytes, Instant uploadedAt) {
    public CourseMaterial {
        Objects.requireNonNull(id, "Material id is required");
        Objects.requireNonNull(courseId, "Course id is required");
        title = requireText(title, "Material title is required", 180);
        originalFileName = requireText(originalFileName, "Original file name is required", 255);
        storageKey = requireText(storageKey, "Storage key is required", 100);
        contentType = requireText(contentType, "Content type is required", 150);
        if (sizeBytes < 1) throw new IllegalArgumentException("Material file must not be empty");
        Objects.requireNonNull(uploadedAt, "Upload time is required");
    }

    private static String requireText(String value, String message, int max) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        var normalized = value.trim();
        if (normalized.length() > max) throw new IllegalArgumentException(message.replace(" is required", " is too long"));
        return normalized;
    }
}
