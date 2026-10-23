package com.devedu.learningplatform.infrastructure.persistence.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "system_settings")
class SystemSettingJpaEntity {

    @Id
    @Column(name = "setting_key", length = 100)
    private String key;

    @Column(name = "setting_value", nullable = false, length = 500)
    private String value;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SystemSettingJpaEntity() {
    }

    SystemSettingJpaEntity(String key, String value, Instant updatedAt) {
        this.key = key;
        this.value = value;
        this.updatedAt = updatedAt;
    }

    String getValue() {
        return value;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}
