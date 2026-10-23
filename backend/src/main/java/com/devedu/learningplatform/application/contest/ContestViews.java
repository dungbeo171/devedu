package com.devedu.learningplatform.application.contest;

import com.devedu.learningplatform.domain.contest.Contest;
import com.devedu.learningplatform.domain.model.*;
import java.time.Instant;
import java.util.*;

public final class ContestViews {
    private ContestViews() {}
    public record Summary(UUID id, String name, Contest.Type type, Instant startsAt, Instant endsAt,
                          int durationMinutes, int problemCount, long participants, Contest.Status status,
                          Contest.Difficulty difficulty, String description, boolean monitoringEnabled,
                          Instant createdAt, Contest.ParticipationMode participationMode) {
        public Summary {
            description = description == null ? "" : description;
            createdAt = createdAt == null ? startsAt : createdAt;
            participationMode = participationMode == null ? Contest.ParticipationMode.OPEN : participationMode;
        }
        public Summary(UUID id, String name, Contest.Type type, Instant startsAt, Instant endsAt, int durationMinutes,
                       int problemCount, long participants, Contest.Status status, Contest.Difficulty difficulty) {
            this(id, name, type, startsAt, endsAt, durationMinutes, problemCount, participants, status,
                    difficulty, "", false, startsAt, Contest.ParticipationMode.OPEN);
        }
        public Summary(UUID id, String name, Contest.Type type, Instant startsAt, Instant endsAt, int durationMinutes,
                       int problemCount, long participants, Contest.Status status, Contest.Difficulty difficulty,
                       String description, boolean monitoringEnabled) {
            this(id, name, type, startsAt, endsAt, durationMinutes, problemCount, participants, status,
                    difficulty, description, monitoringEnabled, startsAt, Contest.ParticipationMode.OPEN);
        }
        public Summary(UUID id, String name, Contest.Type type, Instant startsAt, Instant endsAt, int durationMinutes,
                       int problemCount, long participants, Contest.Status status, Contest.Difficulty difficulty,
                       String description, boolean monitoringEnabled, Instant createdAt) {
            this(id, name, type, startsAt, endsAt, durationMinutes, problemCount, participants, status,
                    difficulty, description, monitoringEnabled, createdAt, Contest.ParticipationMode.OPEN);
        }
    }
    public record Catalog(List<Summary> contests, Instant serverTime) {}
    public record ManagedSummary(UUID id, String name, Contest.Type type, Instant startsAt, Instant endsAt,
                                 int durationMinutes, int problemCount, long participants,
                                 Contest.Status status, Contest.Difficulty difficulty, String description,
                                 boolean monitoringEnabled, boolean published, Contest.Visibility visibility,
                                 Contest.ParticipationMode participationMode, boolean editable, Instant createdAt) {}
    public record ManagedCatalog(List<ManagedSummary> contests, Instant serverTime) {}
    public record UserOption(long id, String name, String email, UserRole role, String code) {}
    public record Manage(UUID id, String name, Contest.Type type, Instant startsAt, Instant endsAt,
                         int durationMinutes, Contest.Difficulty difficulty, String description,
                         boolean monitoringEnabled, boolean published, Contest.Visibility visibility,
                         Contest.ParticipationMode participationMode, List<Contest.Problem> problems,
                         List<UserOption> proctors, List<String> allowedEmails, boolean editable,
                         Instant serverTime, List<LocalProblem> localProblems) {}
    public record LocalProblem(UUID id, String slug, String title, ProblemDifficulty difficulty) {}
    public record Problem(UUID id, String slug, String title, ProblemDifficulty difficulty,
                          String letter, int points, boolean solved, long submissions, boolean available,
                          boolean contestOwned) {
        public Problem(UUID id, String slug, String title, ProblemDifficulty difficulty, String letter,
                       int points, boolean solved, long submissions, boolean available) {
            this(id, slug, title, difficulty, letter, points, solved, submissions, available, false);
        }
    }
    public record ProblemScore(UUID problemId, boolean solved, int score, int attempts,
                               int wrongAttempts, Long solvedAtSeconds) {}
    public record Standing(int rank, long userId, String name, int score, int solved, long timeSeconds,
                           List<ProblemScore> problems) {}
    public record ScoringRules(String name, String scoring, String penalty, String ranking,
                               int wrongAttemptPenaltySeconds, Integer maxAttempts, boolean resubmissionAllowed) {}
    public record Submission(UUID id, UUID problemId, String letter, String title, CodeLanguage language,
                             SubmissionStatus status, int passedTests, int totalTests,
                             long executionTimeMillis, Instant submittedAt, int score, int maxScore) {}
    public record Detail(Summary contest, Instant serverTime, boolean registered,
                         List<Problem> problems, List<Standing> leaderboard, Standing myResult,
                         List<Submission> mySubmissions, ScoringRules scoringRules,
                         com.devedu.learningplatform.domain.contest.ContestAttempt myAttempt) {
        public Detail(Summary contest, Instant serverTime, boolean registered, List<Problem> problems,
                      List<Standing> leaderboard, Standing myResult, List<Submission> mySubmissions, ScoringRules scoringRules) {
            this(contest, serverTime, registered, problems, leaderboard, myResult, mySubmissions, scoringRules, null);
        }
    }
}
