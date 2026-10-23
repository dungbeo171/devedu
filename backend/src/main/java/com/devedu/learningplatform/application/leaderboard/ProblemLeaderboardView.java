package com.devedu.learningplatform.application.leaderboard;

import com.devedu.learningplatform.domain.leaderboard.ProblemLeaderboardPeriod;
import com.devedu.learningplatform.domain.model.UserRole;

import java.time.Instant;
import java.util.List;

public record ProblemLeaderboardView(
        ProblemLeaderboardPeriod period,
        Instant startsAt,
        Instant endsAt,
        Instant generatedAt,
        int easyPoints,
        int mediumPoints,
        int hardPoints,
        List<Entry> entries
) {
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
