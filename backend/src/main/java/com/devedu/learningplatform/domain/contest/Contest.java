package com.devedu.learningplatform.domain.contest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Contest(UUID id, String name, Type type, Instant startsAt, int durationMinutes,
                      UUID createdBy, List<Problem> problems, Difficulty difficulty,
                      Instant endsAt, String description, boolean monitoringEnabled,
                      boolean published, Visibility visibility, ParticipationMode participationMode,
                      Instant createdAt) {
    public Contest(UUID id, String name, Type type, Instant startsAt, int durationMinutes, UUID createdBy,
                   List<Problem> problems, Difficulty difficulty) {
        this(id, name, type, startsAt, durationMinutes, createdBy, problems, difficulty,
                startsAt == null ? null : startsAt.plusSeconds(durationMinutes * 60L), "", false,
                true, Visibility.PUBLIC, ParticipationMode.OPEN, startsAt);
    }
    public Contest(UUID id, String name, Type type, Instant startsAt, int durationMinutes, UUID createdBy,
                   List<Problem> problems, Difficulty difficulty, Instant endsAt, String description,
                   boolean monitoringEnabled) {
        this(id, name, type, startsAt, durationMinutes, createdBy, problems, difficulty, endsAt, description,
                monitoringEnabled, true, Visibility.PUBLIC, ParticipationMode.OPEN, startsAt);
    }
    public Contest(UUID id, String name, Type type, Instant startsAt, int durationMinutes, UUID createdBy,
                   List<Problem> problems, Difficulty difficulty, Instant endsAt, String description,
                   boolean monitoringEnabled, boolean published, Visibility visibility,
                   ParticipationMode participationMode) {
        this(id, name, type, startsAt, durationMinutes, createdBy, problems, difficulty, endsAt, description,
                monitoringEnabled, published, visibility, participationMode, startsAt);
    }
    public enum Difficulty { BEGINNER, INTERMEDIATE, ADVANCED }
    public enum Visibility { PUBLIC, PRIVATE }
    public enum ParticipationMode { OPEN, RESTRICTED }
    public Contest(UUID id, String name, Type type, Instant startsAt, int durationMinutes, UUID createdBy, List<Problem> problems) {
        this(id, name, type, startsAt, durationMinutes, createdBy, problems, Difficulty.BEGINNER);
    }
    public enum Status { UPCOMING, ONGOING, FINISHED }
    public enum Type { WEEKLY, PRACTICE, CUSTOM }
    public record Problem(UUID problemId, int points) {
        public Problem {
            if (problemId == null || points < 1 || points > 10000) {
                throw new IllegalArgumentException("Problem and points (1–10000) are required");
            }
        }
    }

    public Contest {
        difficulty = difficulty == null ? Difficulty.BEGINNER : difficulty;
        if (id == null || createdBy == null || type == null || startsAt == null || createdAt == null) {
            throw new IllegalArgumentException("Contest information is required");
        }
        if (name == null || name.isBlank() || name.trim().length() > 180) {
            throw new IllegalArgumentException("Contest name must contain 1–180 characters");
        }
        name = name.trim();
        description = description == null ? "" : description.trim();
        visibility = visibility == null ? Visibility.PRIVATE : visibility;
        participationMode = participationMode == null ? ParticipationMode.OPEN : participationMode;
        if (description.length() > 5000) throw new IllegalArgumentException("Mô tả không được vượt quá 5.000 ký tự.");
        if (endsAt == null || !endsAt.isAfter(startsAt)) throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu.");
        if (durationMinutes < 1 || durationMinutes > 10080) {
            throw new IllegalArgumentException("Duration must be 1–10080 minutes");
        }
        if (problems == null || problems.size() > 26 || problems.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Select 1–26 existing problems");
        }
        problems = List.copyOf(problems);
        if (published && problems.isEmpty()) throw new IllegalArgumentException("A published contest requires at least one problem");
        if (problems.stream().map(Problem::problemId).distinct().count() != problems.size()) {
            throw new IllegalArgumentException("Contest problems must be unique");
        }
    }

    public Status statusAt(Instant now) {
        if (now.isBefore(startsAt)) return Status.UPCOMING;
        return now.isBefore(endsAt()) ? Status.ONGOING : Status.FINISHED;
    }
}
