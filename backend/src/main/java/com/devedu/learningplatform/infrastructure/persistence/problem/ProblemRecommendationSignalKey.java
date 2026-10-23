package com.devedu.learningplatform.infrastructure.persistence.problem;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
class ProblemRecommendationSignalKey implements Serializable {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "signal_type", nullable = false, length = 20)
    private String signalType;

    @Column(name = "signal_value", nullable = false, length = 30)
    private String signalValue;

    protected ProblemRecommendationSignalKey() {
    }

    ProblemRecommendationSignalKey(UUID userId, String signalType, String signalValue) {
        this.userId = userId;
        this.signalType = signalType;
        this.signalValue = signalValue;
    }

    UUID getUserId() { return userId; }
    String getSignalType() { return signalType; }
    String getSignalValue() { return signalValue; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ProblemRecommendationSignalKey that)) return false;
        return Objects.equals(userId, that.userId)
                && Objects.equals(signalType, that.signalType)
                && Objects.equals(signalValue, that.signalValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, signalType, signalValue);
    }
}
