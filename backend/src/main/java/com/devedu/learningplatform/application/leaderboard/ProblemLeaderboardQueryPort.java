package com.devedu.learningplatform.application.leaderboard;

import com.devedu.learningplatform.domain.model.UserRole;

import java.time.Instant;
import java.util.List;

public interface ProblemLeaderboardQueryPort {
    List<Candidate> findFirstAcceptedSolves(Instant startsAt, Instant endsAt);

    record Candidate(
            long userId,
            String name,
            UserRole role,
            long easySolved,
            long mediumSolved,
            long hardSolved,
            Instant lastAcceptedAt
    ) {
    }
}
