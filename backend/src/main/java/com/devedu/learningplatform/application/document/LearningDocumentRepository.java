package com.devedu.learningplatform.application.document;

import com.devedu.learningplatform.domain.document.LearningDocument;

import java.util.List;
import java.util.Optional;

public interface LearningDocumentRepository {
    List<LearningDocument> findAll();
    Optional<LearningDocument> findBySlug(String slug);
    LearningDocument save(LearningDocument document);
}
