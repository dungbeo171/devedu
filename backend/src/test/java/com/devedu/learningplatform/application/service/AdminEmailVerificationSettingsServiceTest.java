package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.exception.UserManagementForbiddenException;
import com.devedu.learningplatform.application.port.out.EmailVerificationSettingsRepository;
import com.devedu.learningplatform.application.port.out.VerificationEmailSender;
import com.devedu.learningplatform.domain.model.EmailVerificationSettings;
import com.devedu.learningplatform.domain.model.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminEmailVerificationSettingsServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-21T08:00:00Z");
    private final SettingsRepository repository = new SettingsRepository();
    private final AdminEmailVerificationSettingsService service =
            new AdminEmailVerificationSettingsService(
                    repository,
                    new ConfiguredEmailSender(),
                    Clock.fixed(NOW, ZoneOffset.UTC)
            );

    @Test
    void administratorCanPauseAndResumeVerificationEmailDelivery() {
        assertThat(service.get(UserRole.ADMIN).enabled()).isTrue();
        assertThat(service.get(UserRole.ADMIN).smtpConfigured()).isTrue();

        var paused = service.update(false, UserRole.ADMIN);

        assertThat(paused.enabled()).isFalse();
        assertThat(repository.settings.updatedAt()).isEqualTo(NOW);
        assertThat(service.update(true, UserRole.ADMIN).enabled()).isTrue();
    }

    @Test
    void nonAdministratorCannotReadOrChangeTheSetting() {
        assertThatThrownBy(() -> service.get(UserRole.TEACHER))
                .isInstanceOf(UserManagementForbiddenException.class);
        assertThatThrownBy(() -> service.update(false, UserRole.STUDENT))
                .isInstanceOf(UserManagementForbiddenException.class);
    }

    private static final class SettingsRepository implements EmailVerificationSettingsRepository {
        private EmailVerificationSettings settings = new EmailVerificationSettings(true, Instant.EPOCH);

        @Override
        public EmailVerificationSettings get() {
            return settings;
        }

        @Override
        public EmailVerificationSettings update(boolean enabled, Instant updatedAt) {
            settings = new EmailVerificationSettings(enabled, updatedAt);
            return settings;
        }
    }

    private static final class ConfiguredEmailSender implements VerificationEmailSender {
        @Override
        public void sendRegistrationCode(String recipientEmail, String recipientName, String code, Duration validFor) {
        }

        @Override
        public boolean isConfigured() {
            return true;
        }
    }
}
