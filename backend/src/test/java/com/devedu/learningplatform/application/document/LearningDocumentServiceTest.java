package com.devedu.learningplatform.application.document;

import com.devedu.learningplatform.domain.document.LearningDocument;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LearningDocumentServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-23T03:00:00Z");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void prioritizesDocumentsMatchingLearningActivityAndUnseenContent() {
        var java = document("java", List.of("JAVA", "OOP"), 10);
        var python = document("python", List.of("PYTHON"), 20);
        var repository = repository(List.of(python, java));
        LearningDocumentSignalsPort signals = new LearningDocumentSignalsPort() {
            @Override public Signals load(UUID userId) {
                return new Signals(Map.of("JAVA", 8L), Set.of(), Map.of());
            }
            @Override public void recordView(UUID userId, UUID documentId, Instant viewedAt) { }
        };
        var service = new LearningDocumentService(repository, signals, Clock.fixed(NOW, ZoneOffset.UTC));

        var result = service.recommendations(USER_ID);

        assertEquals("java", result.get(0).document().slug());
        assertEquals("Phù hợp với JAVA bạn đang luyện", result.get(0).reason());
    }

    @Test
    void guestReceivesCuratedPriorityOrder() {
        var repository = repository(List.of(document("a", List.of("JAVA"), 1), document("b", List.of("PYTHON"), 9)));
        var service = new LearningDocumentService(repository, new EmptySignals(), Clock.fixed(NOW, ZoneOffset.UTC));

        assertEquals("b", service.recommendations(null).get(0).document().slug());
    }

    private static LearningDocumentRepository repository(List<LearningDocument> values) {
        return new LearningDocumentRepository() {
            @Override public List<LearningDocument> findAll() { return values; }
            @Override public Optional<LearningDocument> findBySlug(String slug) {
                return values.stream().filter(value -> value.slug().equals(slug)).findFirst();
            }
            @Override public LearningDocument save(LearningDocument document) { return document; }
        };
    }

    private static LearningDocument document(String slug, List<String> tags, int priority) {
        return new LearningDocument(UUID.nameUUIDFromBytes(slug.getBytes()), slug, slug, "summary", "content", tags, 5, priority, NOW);
    }

    private static final class EmptySignals implements LearningDocumentSignalsPort {
        @Override public Signals load(UUID userId) { return new Signals(Map.of(), Set.of(), Map.of()); }
        @Override public void recordView(UUID userId, UUID documentId, Instant viewedAt) { }
    }
}
