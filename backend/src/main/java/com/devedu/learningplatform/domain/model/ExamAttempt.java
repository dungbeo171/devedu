package com.devedu.learningplatform.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ExamAttempt(UUID id, UUID examId, UUID studentId, ExamAttemptStatus status,
                          Instant startedAt, Instant expiresAt, Instant submittedAt,
                          int automaticScore, int automaticMaxScore, int pendingCodingQuestions) {
    public ExamAttempt {
        Objects.requireNonNull(id, "Attempt id is required");
        Objects.requireNonNull(examId, "Exam id is required");
        Objects.requireNonNull(studentId, "Student id is required");
        Objects.requireNonNull(status, "Attempt status is required");
        Objects.requireNonNull(startedAt, "Attempt start time is required");
        Objects.requireNonNull(expiresAt, "Attempt expiry time is required");
        if (expiresAt.isBefore(startedAt)) throw new IllegalArgumentException("Attempt expiry time is invalid");
        if (automaticScore < 0 || automaticMaxScore < automaticScore || pendingCodingQuestions < 0) {
            throw new IllegalArgumentException("Attempt result is invalid");
        }
        if (status == ExamAttemptStatus.IN_PROGRESS && submittedAt != null) throw new IllegalArgumentException("In-progress attempt cannot have submitted time");
        if (status == ExamAttemptStatus.SUBMITTED && submittedAt == null) throw new IllegalArgumentException("Submitted attempt requires submitted time");
    }

    public ExamAttempt submit(Instant time, int score, int maxScore, int pendingCoding) {
        return new ExamAttempt(id, examId, studentId, ExamAttemptStatus.SUBMITTED, startedAt, expiresAt,
                time, score, maxScore, pendingCoding);
    }
}
