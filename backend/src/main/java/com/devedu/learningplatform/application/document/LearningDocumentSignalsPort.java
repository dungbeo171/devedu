package com.devedu.learningplatform.application.document;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface LearningDocumentSignalsPort {
    Signals load(UUID userId);
    void recordView(UUID userId, UUID documentId, Instant viewedAt);

    record Signals(Map<String, Long> interests, Set<UUID> viewedDocumentIds, Map<UUID, Long> viewCounts) {
        public Signals {
            interests = Map.copyOf(interests);
            viewedDocumentIds = Set.copyOf(viewedDocumentIds);
            viewCounts = Map.copyOf(viewCounts);
        }
    }
}
