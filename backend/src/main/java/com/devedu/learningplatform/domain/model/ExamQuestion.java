package com.devedu.learningplatform.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ExamQuestion(UUID id, UUID examId, ExamQuestionType type, String prompt,
                           List<String> options, Integer correctOptionIndex, CodeLanguage codingLanguage,
                           int points, int position, Instant createdAt) {
    public ExamQuestion {
        Objects.requireNonNull(id, "Question id is required");
        Objects.requireNonNull(examId, "Exam id is required");
        Objects.requireNonNull(type, "Question type is required");
        if (prompt == null || prompt.isBlank()) throw new IllegalArgumentException("Question prompt is required");
        prompt = prompt.trim();
        if (prompt.length() > 10_000) throw new IllegalArgumentException("Question prompt must not exceed 10000 characters");
        options = options == null ? List.of() : options.stream().map(option -> {
            if (option == null || option.isBlank()) throw new IllegalArgumentException("Question options must not be blank");
            var normalized = option.trim();
            if (normalized.length() > 1000) throw new IllegalArgumentException("Question option must not exceed 1000 characters");
            return normalized;
        }).toList();
        if (points < 1) throw new IllegalArgumentException("Question points must be positive");
        if (position < 1) throw new IllegalArgumentException("Question position must be positive");
        if (type == ExamQuestionType.MULTIPLE_CHOICE) {
            if (options.size() < 2 || options.size() > 6) throw new IllegalArgumentException("Multiple choice question must have 2 to 6 options");
            if (correctOptionIndex == null || correctOptionIndex < 0 || correctOptionIndex >= options.size()) {
                throw new IllegalArgumentException("Correct option index is invalid");
            }
            if (codingLanguage != null) throw new IllegalArgumentException("Multiple choice question cannot have a coding language");
        } else {
            if (!options.isEmpty() || correctOptionIndex != null) throw new IllegalArgumentException("Coding question cannot have multiple choice options");
            Objects.requireNonNull(codingLanguage, "Coding language is required");
        }
        Objects.requireNonNull(createdAt, "Question created time is required");
    }
}
