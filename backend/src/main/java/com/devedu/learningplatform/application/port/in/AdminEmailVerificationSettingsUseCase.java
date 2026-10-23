package com.devedu.learningplatform.application.port.in;

import com.devedu.learningplatform.domain.model.UserRole;

public interface AdminEmailVerificationSettingsUseCase {

    Settings get(UserRole actorRole);

    Settings update(boolean enabled, UserRole actorRole);

    record Settings(boolean enabled, boolean smtpConfigured) {
    }
}
