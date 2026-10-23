package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.leaderboard.ProblemLeaderboardPeriod;
import com.devedu.learningplatform.domain.model.UserRole;

import java.time.Instant;
import java.util.List;

public record ProblemLeaderboardResponse(
        ProblemLeaderboardPeriod period,
        Instant startsAt,
        Instant endsAt,
        Instant generatedAt,
        Scoring scoring,
        List<Entry> entries
) {
    public record Scoring(int easy, int medium, int hard) {
    }

    public record Entry(
            int rank,
            long userId,
            String name,
            UserRole role,
            long score,
            long solved,
            long easySolved,
            long mediumSolved,
            long hardSolved
    ) {
    }
}
