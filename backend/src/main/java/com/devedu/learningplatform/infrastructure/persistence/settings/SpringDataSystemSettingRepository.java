package com.devedu.learningplatform.infrastructure.persistence.settings;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataSystemSettingRepository extends JpaRepository<SystemSettingJpaEntity, String> {
}
