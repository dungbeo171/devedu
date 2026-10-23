package com.devedu.learningplatform.application.analytics;

import java.time.Instant;

public interface AdminAnalyticsQueryPort {
    AdminAnalyticsSnapshot load(Instant now);
}
