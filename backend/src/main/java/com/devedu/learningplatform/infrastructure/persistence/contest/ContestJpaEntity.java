package com.devedu.learningplatform.infrastructure.persistence.contest;

import com.devedu.learningplatform.domain.contest.Contest;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity @Table(name = "contests")
class ContestJpaEntity {
    @Id UUID id;
    @Column(nullable = false, length = 180) String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) Contest.Type type;
    @Column(name = "starts_at", nullable = false) Instant startsAt;
    @Column(name = "ends_at", nullable = false) Instant endsAt;
    @Column(name = "duration_minutes", nullable = false) int durationMinutes;
    @Column(name = "created_by", nullable = false) UUID createdBy;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "deleted_at") Instant deletedAt;
    @Column(nullable = false, length = 5000) String description;
    @Column(name = "monitoring_enabled", nullable = false) boolean monitoringEnabled;
    @Column(nullable = false) boolean published;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) Contest.Visibility visibility;
    @Enumerated(EnumType.STRING) @Column(name = "participation_mode", nullable = false, length = 20) Contest.ParticipationMode participationMode;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) Contest.Difficulty difficulty;
    @ElementCollection
    @CollectionTable(name = "contest_problems", joinColumns = @JoinColumn(name = "contest_id"))
    @OrderColumn(name = "position")
    List<ContestProblemValue> problems = new ArrayList<>();
    protected ContestJpaEntity() {}
    ContestJpaEntity(Contest c) {
        id = c.id(); name = c.name(); type = c.type(); startsAt = c.startsAt(); endsAt = c.endsAt();
        durationMinutes = c.durationMinutes(); createdBy = c.createdBy();
        createdAt = c.createdAt();
        difficulty = c.difficulty();
        description = c.description(); monitoringEnabled = c.monitoringEnabled();
        published = c.published(); visibility = c.visibility(); participationMode = c.participationMode();
        c.problems().forEach(p -> problems.add(new ContestProblemValue(p.problemId(), p.points())));
    }
    Contest toDomain() {
        return new Contest(id, name, type, startsAt, durationMinutes, createdBy,
                problems.stream().map(p -> new Contest.Problem(p.problemId, p.points)).toList(), difficulty, endsAt,
                description, monitoringEnabled, published, visibility, participationMode, createdAt);
    }
}

@Embeddable
class ContestProblemValue {
    @Column(name = "problem_id", nullable = false) UUID problemId;
    @Column(nullable = false) int points;
    protected ContestProblemValue() {}
    ContestProblemValue(UUID problemId, int points) { this.problemId = problemId; this.points = points; }
}
