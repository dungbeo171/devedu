package com.devedu.learningplatform.infrastructure.persistence.settings;

import com.devedu.learningplatform.application.port.out.EmailVerificationSettingsRepository;
import com.devedu.learningplatform.domain.model.EmailVerificationSettings;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public class EmailVerificationSettingsRepositoryAdapter
        implements EmailVerificationSettingsRepository {

    private static final String KEY = "registration_email_verification_enabled";
    private final SpringDataSystemSettingRepository repository;

    public EmailVerificationSettingsRepositoryAdapter(SpringDataSystemSettingRepository repository) {
        this.repository = repository;
    }

    @Override
    public EmailVerificationSettings get() {
        return repository.findById(KEY)
                .map(entity -> new EmailVerificationSettings(
                        Boolean.parseBoolean(entity.getValue()), entity.getUpdatedAt()))
                .orElseGet(() -> new EmailVerificationSettings(true, Instant.EPOCH));
    }

    @Override
    public EmailVerificationSettings update(boolean enabled, Instant updatedAt) {
        var saved = repository.saveAndFlush(new SystemSettingJpaEntity(
                KEY, Boolean.toString(enabled), updatedAt
        ));
        return new EmailVerificationSettings(
                Boolean.parseBoolean(saved.getValue()), saved.getUpdatedAt()
        );
    }
}
