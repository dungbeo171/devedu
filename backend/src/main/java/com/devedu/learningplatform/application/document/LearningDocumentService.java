package com.devedu.learningplatform.application.document;

import com.devedu.learningplatform.domain.document.LearningDocument;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class LearningDocumentService implements LearningDocumentUseCase {
    private final LearningDocumentRepository documents;
    private final LearningDocumentSignalsPort signals;
    private final Clock clock;

    public LearningDocumentService(LearningDocumentRepository documents, LearningDocumentSignalsPort signals, Clock clock) {
        this.documents = Objects.requireNonNull(documents);
        this.signals = Objects.requireNonNull(signals);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public List<LearningDocument> list() {
        return documents.findAll().stream()
                .sorted(Comparator.comparingInt(LearningDocument::priority).reversed()
                        .thenComparing(LearningDocument::title))
                .toList();
    }

    @Override
    public LearningDocument get(String slug) {
        if (slug == null || slug.isBlank()) throw new IllegalArgumentException("Slug tài liệu là bắt buộc");
        return documents.findBySlug(slug).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài liệu"));
    }

    @Override
    public List<LearningDocumentViews.Recommendation> recommendations(UUID userId) {
        var catalog = list();
        if (userId == null) {
            return catalog.stream().map(document ->
                    new LearningDocumentViews.Recommendation(document, "Nội dung nền tảng được DevEdu đề xuất")).toList();
        }
        var userSignals = signals.load(userId);
        return catalog.stream()
                .map(document -> score(document, userSignals))
                .sorted(Comparator.comparingLong(ScoredDocument::score).reversed()
                        .thenComparing(item -> item.document().title()))
                .map(item -> new LearningDocumentViews.Recommendation(item.document(), item.reason()))
                .toList();
    }

    @Override
    public void recordView(UUID userId, String slug) {
        Objects.requireNonNull(userId, "Người dùng là bắt buộc");
        signals.recordView(userId, get(slug).id(), clock.instant());
    }

    private ScoredDocument score(LearningDocument document, LearningDocumentSignalsPort.Signals userSignals) {
        long score = document.priority();
        String strongestTag = null;
        long strongestSignal = 0;
        for (String tag : document.tags()) {
            long strength = userSignals.interests().getOrDefault(tag.toUpperCase(Locale.ROOT), 0L);
            score += Math.min(strength, 20) * 5;
            if (strength > strongestSignal) {
                strongestSignal = strength;
                strongestTag = tag;
            }
        }
        if (!userSignals.viewedDocumentIds().contains(document.id())) score += 15;
        score -= Math.min(userSignals.viewCounts().getOrDefault(document.id(), 0L), 5) * 2;
        String reason = strongestTag == null
                ? "Nội dung tiếp theo phù hợp để mở rộng kiến thức"
                : "Phù hợp với " + label(strongestTag) + " bạn đang luyện";
        return new ScoredDocument(document, score, reason);
    }

    private String label(String tag) {
        return switch (tag.toUpperCase(Locale.ROOT)) {
            case "CPP" -> "C++";
            case "DATA_STRUCTURES" -> "Cấu trúc dữ liệu";
            case "ALGORITHMS" -> "Thuật toán";
            case "INTRODUCTION", "BEGINNER" -> "Nhập môn lập trình";
            case "HTML", "WEB" -> "Web";
            case "LEARNING" -> "kỹ năng học tập";
            default -> tag;
        };
    }

    private record ScoredDocument(LearningDocument document, long score, String reason) {
    }
}
