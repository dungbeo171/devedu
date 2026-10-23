package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.domain.model.EmailVerificationSettings;

import java.time.Instant;

public interface EmailVerificationSettingsRepository {

    EmailVerificationSettings get();

    EmailVerificationSettings update(boolean enabled, Instant updatedAt);
}
