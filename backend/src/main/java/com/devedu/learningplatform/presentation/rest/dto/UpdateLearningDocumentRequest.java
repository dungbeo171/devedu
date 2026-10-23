package com.devedu.learningplatform.presentation.rest.dto;

import java.util.List;

public record UpdateLearningDocumentRequest(
        String title,
        String summary,
        String content,
        List<String> tags,
        int readingMinutes,
        int priority
) {
}
