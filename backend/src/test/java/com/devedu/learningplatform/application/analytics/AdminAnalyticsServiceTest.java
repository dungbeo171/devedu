package com.devedu.learningplatform.application.analytics;

import com.devedu.learningplatform.domain.model.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminAnalyticsServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-09T00:00:00Z");

    @Test
    void onlyAdminCanLoadDashboard() {
        AdminAnalyticsService service = new AdminAnalyticsService(now -> snapshot(now), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThrows(IllegalArgumentException.class, () -> service.dashboard(UserRole.STUDENT));
        assertThrows(IllegalArgumentException.class, () -> service.dashboard(UserRole.TEACHER));
        assertEquals(NOW, service.dashboard(UserRole.ADMIN).generatedAt());
    }

    private static AdminAnalyticsSnapshot snapshot(Instant now) {
        return new AdminAnalyticsSnapshot(
                now,
                new AdminAnalyticsSnapshot.Users(0, 0, 0, 0, 0),
                new AdminAnalyticsSnapshot.Problems(0, 0, 0, 0, 0),
                new AdminAnalyticsSnapshot.Activity(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                new AdminAnalyticsSnapshot.Contests(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                new AdminAnalyticsSnapshot.Monitoring(0, 0, 0, 0, 0, 0, 0),
                List.of(), List.of(), List.of(), List.of(), List.of()
        );
    }
}
