package com.devedu.learningplatform.infrastructure.persistence.problem;

import com.devedu.learningplatform.domain.model.ProblemTopic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "programming_problems")
class ProgrammingProblemJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "sample_input", nullable = false, columnDefinition = "TEXT")
    private String sampleInput;

    @Column(name = "sample_output", nullable = false, columnDefinition = "TEXT")
    private String sampleOutput;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProblemTopic topic;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProgrammingProblemJpaEntity() {
    }

    UUID getId() {
        return id;
    }

    String getSlug() {
        return slug;
    }

    String getTitle() {
        return title;
    }

    String getSummary() {
        return summary;
    }

    String getDescription() {
        return description;
    }

    String getSampleInput() {
        return sampleInput;
    }

    String getSampleOutput() {
        return sampleOutput;
    }

    ProblemTopic getTopic() {
        return topic;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
