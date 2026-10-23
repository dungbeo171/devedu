package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.document.LearningDocument;

import java.time.Instant;
import java.util.List;

public record LearningDocumentResponse(
        String slug,
        String title,
        String summary,
        String content,
        List<String> tags,
        int readingMinutes,
        int priority,
        Instant createdAt,
        String recommendationReason
) {
    public static LearningDocumentResponse summary(LearningDocument document, String reason) {
        return new LearningDocumentResponse(
                document.slug(), document.title(), document.summary(), null, document.tags(),
                document.readingMinutes(), document.priority(), document.createdAt(), reason);
    }

    public static LearningDocumentResponse detail(LearningDocument document) {
        return new LearningDocumentResponse(
                document.slug(), document.title(), document.summary(), document.content(), document.tags(),
                document.readingMinutes(), document.priority(), document.createdAt(), null);
    }
}
