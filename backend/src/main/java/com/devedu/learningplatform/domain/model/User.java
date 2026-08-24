package com.devedu.learningplatform.domain.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public record User(
        UUID id,
        String email,
        String passwordHash,
        UserRole role,
        Instant createdAt
) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public User {
        Objects.requireNonNull(id, "User id is required");
        email = normalizeEmail(email);
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash is required");
        }
        Objects.requireNonNull(role, "User role is required");
        Objects.requireNonNull(createdAt, "Created time is required");
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
}

