package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.CourseMaterial;
import java.time.Instant;
import java.util.UUID;

public record CourseMaterialResponse(UUID id, String title, String fileName, String contentType,
                                     long sizeBytes, Instant uploadedAt) {
    public static CourseMaterialResponse from(CourseMaterial material) {
        return new CourseMaterialResponse(material.id(), material.title(), material.originalFileName(),
                material.contentType(), material.sizeBytes(), material.uploadedAt());
    }
}
