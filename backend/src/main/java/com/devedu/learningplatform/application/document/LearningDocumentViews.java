package com.devedu.learningplatform.application.document;

import com.devedu.learningplatform.domain.document.LearningDocument;

public final class LearningDocumentViews {
    private LearningDocumentViews() {
    }

    public record Recommendation(LearningDocument document, String reason) {
    }
}
