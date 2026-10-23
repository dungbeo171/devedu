package com.devedu.learningplatform.application.analytics;

import com.devedu.learningplatform.domain.model.UserRole;

public interface AdminAnalyticsUseCase {
    AdminAnalyticsSnapshot dashboard(UserRole actorRole);
}
