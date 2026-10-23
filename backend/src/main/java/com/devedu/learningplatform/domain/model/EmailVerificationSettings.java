package com.devedu.learningplatform.domain.model;

import java.time.Instant;
import java.util.Objects;

public record EmailVerificationSettings(boolean enabled, Instant updatedAt) {

    public EmailVerificationSettings {
        Objects.requireNonNull(updatedAt, "Updated time is required");
    }
}
