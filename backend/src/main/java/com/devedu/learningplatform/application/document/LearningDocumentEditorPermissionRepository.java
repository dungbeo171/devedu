package com.devedu.learningplatform.application.document;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public interface LearningDocumentEditorPermissionRepository {
    boolean existsByUserId(UUID userId);
    Set<UUID> findAllUserIds();
    void set(UUID userId, boolean enabled, Instant changedAt);
}
