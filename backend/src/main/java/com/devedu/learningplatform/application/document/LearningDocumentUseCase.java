package com.devedu.learningplatform.application.document;

import com.devedu.learningplatform.domain.document.LearningDocument;

import java.util.List;
import java.util.UUID;

public interface LearningDocumentUseCase {
    List<LearningDocument> list();
    LearningDocument get(String slug);
    List<LearningDocumentViews.Recommendation> recommendations(UUID userId);
    void recordView(UUID userId, String slug);
}
