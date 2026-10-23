package com.devedu.learningplatform.application.analytics;

import com.devedu.learningplatform.domain.model.UserRole;

import java.time.Clock;
import java.util.Objects;

public final class AdminAnalyticsService implements AdminAnalyticsUseCase {
    private final AdminAnalyticsQueryPort queryPort;
    private final Clock clock;

    public AdminAnalyticsService(AdminAnalyticsQueryPort queryPort, Clock clock) {
        this.queryPort = Objects.requireNonNull(queryPort);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public AdminAnalyticsSnapshot dashboard(UserRole actorRole) {
        if (actorRole != UserRole.ADMIN) {
            throw new IllegalArgumentException("Only administrators can view system analytics");
        }
        return queryPort.load(clock.instant());
    }
}
