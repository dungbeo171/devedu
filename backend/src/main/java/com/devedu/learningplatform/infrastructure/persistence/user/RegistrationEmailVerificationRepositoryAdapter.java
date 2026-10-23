package com.devedu.learningplatform.infrastructure.persistence.user;

import com.devedu.learningplatform.application.port.out.RegistrationEmailVerificationRepository;
import com.devedu.learningplatform.domain.model.RegistrationEmailVerification;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class RegistrationEmailVerificationRepositoryAdapter
        implements RegistrationEmailVerificationRepository {

    private final SpringDataRegistrationEmailVerificationRepository repository;

    public RegistrationEmailVerificationRepositoryAdapter(
            SpringDataRegistrationEmailVerificationRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public Optional<RegistrationEmailVerification> findByEmail(String email) {
        return repository.findById(email).map(this::toDomain);
    }

    @Override
    public RegistrationEmailVerification save(RegistrationEmailVerification verification) {
        var saved = repository.saveAndFlush(new RegistrationEmailVerificationJpaEntity(
                verification.email(),
                verification.name(),
                verification.passwordHash(),
                verification.codeHash(),
                verification.expiresAt(),
                verification.resendAvailableAt(),
                verification.failedAttempts(),
                verification.createdAt()
        ));
        return toDomain(saved);
    }

    @Override
    public void deleteByEmail(String email) {
        repository.deleteById(email);
    }

    private RegistrationEmailVerification toDomain(RegistrationEmailVerificationJpaEntity entity) {
        return new RegistrationEmailVerification(
                entity.getEmail(),
                entity.getName(),
                entity.getPasswordHash(),
                entity.getCodeHash(),
                entity.getExpiresAt(),
                entity.getResendAvailableAt(),
                entity.getFailedAttempts(),
                entity.getCreatedAt()
        );
    }
}
