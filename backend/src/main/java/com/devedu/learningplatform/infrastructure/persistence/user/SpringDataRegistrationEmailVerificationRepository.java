package com.devedu.learningplatform.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataRegistrationEmailVerificationRepository
        extends JpaRepository<RegistrationEmailVerificationJpaEntity, String> {
}
