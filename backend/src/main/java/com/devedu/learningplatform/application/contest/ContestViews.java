package com.devedu.learningplatform.application.contest;

import com.devedu.learningplatform.domain.contest.Contest;
import com.devedu.learningplatform.domain.model.*;
import java.time.Instant;
import java.util.*;

public final class ContestViews {
    private ContestViews() {}
    public record Summary(UUID id, String name, Contest.Type type, Instant startsAt, Instant endsAt,
                          int durationMinutes, int problemCount, long participants, Contest.Status status,
                          boolean rated, Contest.Difficulty difficulty) {}
    public record Catalog(List<Summary> contests, Instant serverTime) {}
    public record Problem(UUID id, String slug, String title, ProblemDifficulty difficulty,
                          String letter, int points, boolean solved, long submissions, boolean available) {}
    public record ProblemScore(UUID problemId, boolean solved, int score, int attempts,
                               int wrongAttempts, Long solvedAtSeconds) {}
    public record Standing(int rank, long userId, String name, int score, int solved, long timeSeconds,
                           List<ProblemScore> problems) {}
    public record ScoringRules(String name, String scoring, String penalty, String ranking,
                               int wrongAttemptPenaltySeconds, Integer maxAttempts, boolean resubmissionAllowed) {}
    public record Submission(UUID id, UUID problemId, String letter, String title, CodeLanguage language,
                             SubmissionStatus status, int passedTests, int totalTests,
                             long executionTimeMillis, Instant submittedAt, int score, int maxScore) {}
    public record Detail(Summary contest, Instant serverTime, boolean registered, String rules,
                         List<Problem> problems, List<Standing> leaderboard, Standing myResult,
                         List<Submission> mySubmissions, ScoringRules scoringRules) {}
}
