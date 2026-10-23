package com.devedu.learningplatform.infrastructure.persistence.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "registration_email_verifications")
class RegistrationEmailVerificationJpaEntity {

    @Id
    @Column(length = 254)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Column(name = "code_hash", nullable = false, length = 60)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "resend_available_at", nullable = false)
    private Instant resendAvailableAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RegistrationEmailVerificationJpaEntity() {
    }

    RegistrationEmailVerificationJpaEntity(
            String email,
            String name,
            String passwordHash,
            String codeHash,
            Instant expiresAt,
            Instant resendAvailableAt,
            int failedAttempts,
            Instant createdAt
    ) {
        this.email = email;
        this.name = name;
        this.passwordHash = passwordHash;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.resendAvailableAt = resendAvailableAt;
        this.failedAttempts = failedAttempts;
        this.createdAt = createdAt;
    }

    String getEmail() { return email; }

    String getName() { return name; }

    String getPasswordHash() { return passwordHash; }

    String getCodeHash() { return codeHash; }

    Instant getExpiresAt() { return expiresAt; }

    Instant getResendAvailableAt() { return resendAvailableAt; }

    int getFailedAttempts() { return failedAttempts; }

    Instant getCreatedAt() { return createdAt; }
}
