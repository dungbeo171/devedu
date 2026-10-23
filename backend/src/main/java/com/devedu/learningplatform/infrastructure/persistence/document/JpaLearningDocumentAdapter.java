package com.devedu.learningplatform.infrastructure.persistence.document;

import com.devedu.learningplatform.application.document.LearningDocumentRepository;
import com.devedu.learningplatform.application.document.LearningDocumentSignalsPort;
import com.devedu.learningplatform.application.document.LearningDocumentEditorPermissionRepository;
import com.devedu.learningplatform.domain.document.LearningDocument;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class JpaLearningDocumentAdapter implements LearningDocumentRepository, LearningDocumentSignalsPort,
        LearningDocumentEditorPermissionRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<LearningDocument> findAll() {
        List<?> rows = entityManager.createNativeQuery("""
                SELECT id, slug, title, summary, content, tags, reading_minutes, priority, created_at
                FROM learning_documents
                WHERE published = TRUE
                ORDER BY priority DESC, title
                """).getResultList();
        return rows.stream().map(this::document).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LearningDocument> findBySlug(String slug) {
        var query = entityManager.createNativeQuery("""
                SELECT id, slug, title, summary, content, tags, reading_minutes, priority, created_at
                FROM learning_documents
                WHERE slug = :slug AND published = TRUE
                """);
        query.setParameter("slug", slug);
        List<?> rows = query.getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(document(rows.get(0)));
    }

    @Override
    public LearningDocument save(LearningDocument document) {
        int updated = entityManager.createNativeQuery("""
                UPDATE learning_documents
                SET title = :title,
                    summary = :summary,
                    content = :content,
                    tags = :tags,
                    reading_minutes = :readingMinutes,
                    priority = :priority
                WHERE id = :id AND published = TRUE
                """)
                .setParameter("title", document.title())
                .setParameter("summary", document.summary())
                .setParameter("content", document.content())
                .setParameter("tags", String.join(",", document.tags()))
                .setParameter("readingMinutes", document.readingMinutes())
                .setParameter("priority", document.priority())
                .setParameter("id", document.id())
                .executeUpdate();
        if (updated != 1) throw new IllegalArgumentException("Không tìm thấy tài liệu");
        return document;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserId(UUID userId) {
        Object value = entityManager.createNativeQuery("""
                SELECT EXISTS (
                    SELECT 1 FROM learning_document_editor_permissions WHERE user_id = :userId
                )
                """).setParameter("userId", userId).getSingleResult();
        return Boolean.TRUE.equals(value);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Set<UUID> findAllUserIds() {
        var result = new java.util.HashSet<UUID>();
        for (Object value : entityManager.createNativeQuery(
                "SELECT user_id FROM learning_document_editor_permissions").getResultList()) {
            result.add((UUID) value);
        }
        return result;
    }

    @Override
    public void set(UUID userId, boolean enabled, Instant changedAt) {
        if (!enabled) {
            entityManager.createNativeQuery(
                    "DELETE FROM learning_document_editor_permissions WHERE user_id = :userId")
                    .setParameter("userId", userId)
                    .executeUpdate();
            return;
        }
        entityManager.createNativeQuery("""
                INSERT INTO learning_document_editor_permissions (user_id, granted_at)
                VALUES (:userId, :grantedAt)
                ON CONFLICT (user_id) DO UPDATE SET granted_at = EXCLUDED.granted_at
                """)
                .setParameter("userId", userId)
                .setParameter("grantedAt", changedAt)
                .executeUpdate();
    }

    @Override
    @Transactional(readOnly = true)
    public Signals load(UUID userId) {
        Map<String, Long> interests = new HashMap<>();
        var languageQuery = entityManager.createNativeQuery("""
                SELECT language, COUNT(*) FROM problem_submissions
                WHERE student_id = :userId GROUP BY language
                """);
        languageQuery.setParameter("userId", userId);
        for (Object value : languageQuery.getResultList()) {
            Object[] row = (Object[]) value;
            interests.merge(String.valueOf(row[0]), number(row[1]), Long::sum);
        }
        var topicQuery = entityManager.createNativeQuery("""
                SELECT p.topic, COUNT(*)
                FROM problem_submissions s
                JOIN programming_problems p ON p.id = s.problem_id
                WHERE s.student_id = :userId
                GROUP BY p.topic
                """);
        topicQuery.setParameter("userId", userId);
        for (Object value : topicQuery.getResultList()) {
            Object[] row = (Object[]) value;
            interests.merge(String.valueOf(row[0]), number(row[1]), Long::sum);
        }

        var viewQuery = entityManager.createNativeQuery("""
                SELECT document_id, view_count FROM learning_document_views WHERE user_id = :userId
                """);
        viewQuery.setParameter("userId", userId);
        var viewedIds = new HashSet<UUID>();
        var viewCounts = new HashMap<UUID, Long>();
        for (Object value : viewQuery.getResultList()) {
            Object[] row = (Object[]) value;
            UUID documentId = (UUID) row[0];
            viewedIds.add(documentId);
            viewCounts.put(documentId, number(row[1]));
        }
        return new Signals(interests, viewedIds, viewCounts);
    }

    @Override
    public void recordView(UUID userId, UUID documentId, Instant viewedAt) {
        entityManager.createNativeQuery("""
                INSERT INTO learning_document_views (user_id, document_id, view_count, last_viewed_at)
                VALUES (:userId, :documentId, 1, :viewedAt)
                ON CONFLICT (user_id, document_id) DO UPDATE
                SET view_count = learning_document_views.view_count + 1,
                    last_viewed_at = EXCLUDED.last_viewed_at
                """)
                .setParameter("userId", userId)
                .setParameter("documentId", documentId)
                .setParameter("viewedAt", viewedAt)
                .executeUpdate();
    }

    private LearningDocument document(Object value) {
        Object[] row = (Object[]) value;
        List<String> tags = new ArrayList<>();
        for (String tag : String.valueOf(row[5]).split(",")) {
            if (!tag.isBlank()) tags.add(tag.trim());
        }
        return new LearningDocument(
                (UUID) row[0], String.valueOf(row[1]), String.valueOf(row[2]), String.valueOf(row[3]),
                String.valueOf(row[4]), tags, ((Number) row[6]).intValue(), ((Number) row[7]).intValue(),
                instant(row[8]));
    }

    private static long number(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }

    private static Instant instant(Object value) {
        if (value instanceof Instant instant) return instant;
        if (value instanceof OffsetDateTime offsetDateTime) return offsetDateTime.toInstant();
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toInstant();
        throw new IllegalStateException("Thời gian tài liệu không hợp lệ");
    }
}
