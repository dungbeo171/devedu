package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.exception.UserManagementForbiddenException;
import com.devedu.learningplatform.application.port.in.AdminEmailVerificationSettingsUseCase;
import com.devedu.learningplatform.application.port.out.EmailVerificationSettingsRepository;
import com.devedu.learningplatform.application.port.out.VerificationEmailSender;
import com.devedu.learningplatform.domain.model.UserRole;

import java.time.Clock;
import java.time.Instant;

public final class AdminEmailVerificationSettingsService
        implements AdminEmailVerificationSettingsUseCase {

    private final EmailVerificationSettingsRepository repository;
    private final VerificationEmailSender emailSender;
    private final Clock clock;

    public AdminEmailVerificationSettingsService(
            EmailVerificationSettingsRepository repository,
            VerificationEmailSender emailSender,
            Clock clock
    ) {
        this.repository = repository;
        this.emailSender = emailSender;
        this.clock = clock;
    }

    @Override
    public Settings get(UserRole actorRole) {
        requireAdmin(actorRole);
        return toView(repository.get().enabled());
    }

    @Override
    public Settings update(boolean enabled, UserRole actorRole) {
        requireAdmin(actorRole);
        return toView(repository.update(enabled, Instant.now(clock)).enabled());
    }

    private Settings toView(boolean enabled) {
        return new Settings(enabled, emailSender.isConfigured());
    }

    private void requireAdmin(UserRole actorRole) {
        if (actorRole != UserRole.ADMIN) {
            throw new UserManagementForbiddenException("Only administrators can manage email verification settings");
        }
    }
}
