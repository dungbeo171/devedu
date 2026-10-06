package com.devedu.learningplatform.application.contest;

import com.devedu.learningplatform.application.contest.rating.RatingCalculator.Change;
import com.devedu.learningplatform.domain.contest.VirtualContest;
import java.time.Instant;
import java.util.*;

public final class ContestExperienceViews {
    private ContestExperienceViews() {}
    public record Result(ContestViews.Detail detail, Instant serverTime, boolean finalized, Change rating,
                         int wrongAttempts, double averageSolveSeconds) {}
    public record Session(UUID id, UUID contestId, Instant startsAt, Instant endsAt) {}
    public record VirtualSession(Session session, ContestViews.Detail detail, Instant serverTime) {}
    public record History(UUID contestId, String name, Instant endedAt, int rank, int score, int solved,
                          long timeSeconds, Change rating) {}
    public record VirtualHistory(UUID id, UUID contestId, String name, Instant startsAt, Instant endsAt) {}
    public record Profile(long userId, String name, String handle, Instant serverTime, int currentRating,
                          int previousRating, int ratingChange, int peakRating, int participated, int wins,
                          int top10, int top100, Integer bestRank, double averageRank, int virtualContests,
                          List<History> history, List<VirtualHistory> virtualHistory) {}
}
