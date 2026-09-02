package com.devedu.learningplatform.domain.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public record User(
        UUID id,
        long publicId,
        String studentCode,
        String teacherCode,
        String name,
        String email,
        String passwordHash,
        UserRole role,
        Instant createdAt
) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public User {
        Objects.requireNonNull(id, "User id is required");
        Objects.requireNonNull(role, "User role is required");
        if (publicId < 0 && role != UserRole.ADMIN) {
            throw new IllegalArgumentException("Academic user id must not be negative");
        }
        name = normalizeName(name);
        email = normalizeEmail(email);
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash is required");
        }
        Objects.requireNonNull(createdAt, "Created time is required");
    }

    public User(UUID id, String name, String email, String passwordHash, UserRole role, Instant createdAt) {
        this(id, 0, null, null, name, email, passwordHash, role, createdAt);
    }

    public static String normalizeEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Email is required");
        }

        var normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.length() > 254 || !EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new IllegalArgumentException("Email is invalid");
        }
        return normalizedEmail;
    }

    public static String normalizeName(String name) {
        if (name == null) throw new IllegalArgumentException("Name is required");
        var normalizedName = name.trim().replaceAll("\\s+", " ");
        if (normalizedName.isBlank() || normalizedName.length() > 100) {
            throw new IllegalArgumentException("Name must contain between 1 and 100 characters");
        }
        return normalizedName;
    }
}
