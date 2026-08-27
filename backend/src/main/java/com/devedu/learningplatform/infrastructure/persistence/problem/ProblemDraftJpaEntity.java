package com.devedu.learningplatform.infrastructure.persistence.problem;

import com.devedu.learningplatform.domain.model.CodeLanguage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "problem_drafts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_problem_drafts_student_problem",
                columnNames = {"student_id", "problem_id"}
        )
)
class ProblemDraftJpaEntity {

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

    @Column(nullable = false, columnDefinition = "TEXT")
    private String input;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProblemDraftJpaEntity() {
    }

    ProblemDraftJpaEntity(UUID id, UUID problemId, UUID studentId) {
        this.id = id;
        this.problemId = problemId;
        this.studentId = studentId;
    }

    void update(CodeLanguage language, String sourceCode, String input, Instant updatedAt) {
        this.language = language;
        this.sourceCode = sourceCode;
        this.input = input;
        this.updatedAt = updatedAt;
    }

    UUID getProblemId() { return problemId; }
    UUID getStudentId() { return studentId; }
    CodeLanguage getLanguage() { return language; }
    String getSourceCode() { return sourceCode; }
    String getInput() { return input; }
    Instant getUpdatedAt() { return updatedAt; }
}
