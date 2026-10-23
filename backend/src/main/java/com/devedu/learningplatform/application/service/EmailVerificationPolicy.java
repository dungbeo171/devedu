package com.devedu.learningplatform.application.service;

import java.time.Duration;
import java.util.Objects;

public record EmailVerificationPolicy(Duration codeTtl, Duration resendCooldown, int maxAttempts) {

    public EmailVerificationPolicy {
        Objects.requireNonNull(codeTtl, "Code TTL is required");
        Objects.requireNonNull(resendCooldown, "Resend cooldown is required");
        if (codeTtl.isZero() || codeTtl.isNegative()) {
            throw new IllegalArgumentException("Code TTL must be positive");
        }
        if (resendCooldown.isNegative() || resendCooldown.compareTo(codeTtl) >= 0) {
            throw new IllegalArgumentException("Resend cooldown must be non-negative and shorter than code TTL");
        }
        if (maxAttempts < 1 || maxAttempts > 20) {
            throw new IllegalArgumentException("Maximum attempts must be between 1 and 20");
        }
    }
}
