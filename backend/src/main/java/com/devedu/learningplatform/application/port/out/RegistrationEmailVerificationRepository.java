package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.domain.model.RegistrationEmailVerification;

import java.util.Optional;

public interface RegistrationEmailVerificationRepository {

    Optional<RegistrationEmailVerification> findByEmail(String email);

    RegistrationEmailVerification save(RegistrationEmailVerification verification);

    void deleteByEmail(String email);
}
