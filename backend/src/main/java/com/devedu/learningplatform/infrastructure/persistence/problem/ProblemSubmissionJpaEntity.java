package com.devedu.learningplatform.infrastructure.persistence.problem;

import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.SubmissionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "problem_submissions")
class ProblemSubmissionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "problem_id", nullable = false)
    private UUID problemId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CodeLanguage language;

    @Column(name = "source_code", nullable = false, columnDefinition = "TEXT")
    private String sourceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubmissionStatus status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String diagnostic;

    @Column(name = "passed_tests", nullable = false)
    private int passedTests;

    @Column(name = "total_tests", nullable = false)
    private int totalTests;

    @Column(name = "execution_time_ms", nullable = false)
    private long executionTimeMillis;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    protected ProblemSubmissionJpaEntity() {
    }

    ProblemSubmissionJpaEntity(
            UUID id,
            UUID problemId,
            UUID studentId,
            CodeLanguage language,
            String sourceCode,
            SubmissionStatus status,
            String diagnostic,
            int passedTests,
            int totalTests,
            long executionTimeMillis,
            Instant submittedAt
    ) {
        this.id = id;
        this.problemId = problemId;
        this.studentId = studentId;
        this.language = language;
        this.sourceCode = sourceCode;
        this.status = status;
        this.diagnostic = diagnostic;
        this.passedTests = passedTests;
        this.totalTests = totalTests;
        this.executionTimeMillis = executionTimeMillis;
        this.submittedAt = submittedAt;
    }

    UUID getId() {
        return id;
    }

    UUID getProblemId() {
        return problemId;
    }

    UUID getStudentId() {
        return studentId;
    }

    CodeLanguage getLanguage() {
        return language;
    }

    String getSourceCode() {
        return sourceCode;
    }

    SubmissionStatus getStatus() {
        return status;
    }

    String getDiagnostic() { return diagnostic; }
    int getPassedTests() { return passedTests; }
    int getTotalTests() { return totalTests; }
    long getExecutionTimeMillis() { return executionTimeMillis; }

    Instant getSubmittedAt() {
        return submittedAt;
    }
}
