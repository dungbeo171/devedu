package com.devedu.learningplatform.domain.model;

import java.time.Instant;
import java.util.Objects;

public record RegistrationEmailVerification(
        String email,
        String name,
        String passwordHash,
        String codeHash,
        Instant expiresAt,
        Instant resendAvailableAt,
        int failedAttempts,
        Instant createdAt
) {

    public RegistrationEmailVerification {
        Objects.requireNonNull(email, "Email is required");
        Objects.requireNonNull(name, "Name is required");
        Objects.requireNonNull(passwordHash, "Password hash is required");
        Objects.requireNonNull(codeHash, "Verification code hash is required");
        Objects.requireNonNull(expiresAt, "Expiration time is required");
        Objects.requireNonNull(resendAvailableAt, "Resend time is required");
        Objects.requireNonNull(createdAt, "Creation time is required");
        if (failedAttempts < 0) {
            throw new IllegalArgumentException("Failed attempts cannot be negative");
        }
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean canResend(Instant now) {
        return !now.isBefore(resendAvailableAt);
    }

    public RegistrationEmailVerification withFailedAttempts(int attempts) {
        return new RegistrationEmailVerification(
                email, name, passwordHash, codeHash, expiresAt, resendAvailableAt, attempts, createdAt
        );
    }
}
