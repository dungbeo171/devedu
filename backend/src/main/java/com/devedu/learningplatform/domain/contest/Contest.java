package com.devedu.learningplatform.domain.contest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Contest(UUID id, String name, Type type, Instant startsAt, int durationMinutes,
                      UUID createdBy, String rules, List<Problem> problems, boolean rated, Difficulty difficulty) {
    public enum Difficulty { BEGINNER, INTERMEDIATE, ADVANCED }
    public Contest(UUID id, String name, Type type, Instant startsAt, int durationMinutes, UUID createdBy, String rules, List<Problem> problems) {
        this(id, name, type, startsAt, durationMinutes, createdBy, rules, problems, false, Difficulty.BEGINNER);
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
        if (id == null || createdBy == null || type == null || startsAt == null) {
            throw new IllegalArgumentException("Contest information is required");
        }
        if (name == null || name.isBlank() || name.trim().length() > 180) {
            throw new IllegalArgumentException("Contest name must contain 1–180 characters");
        }
        name = name.trim();
        rules = rules == null ? "" : rules.trim();
        if (rules.length() > 10000 || durationMinutes < 1 || durationMinutes > 10080) {
            throw new IllegalArgumentException("Rules must not exceed 10000 characters; duration must be 1–10080 minutes");
        }
        if (problems == null || problems.isEmpty() || problems.size() > 26 || problems.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Select 1–26 existing problems");
        }
        problems = List.copyOf(problems);
        if (problems.stream().map(Problem::problemId).distinct().count() != problems.size()) {
            throw new IllegalArgumentException("Contest problems must be unique");
        }
    }

    public Instant endsAt() { return startsAt.plusSeconds(durationMinutes * 60L); }
    public Status statusAt(Instant now) {
        if (now.isBefore(startsAt)) return Status.UPCOMING;
        return now.isBefore(endsAt()) ? Status.ONGOING : Status.FINISHED;
    }
}
