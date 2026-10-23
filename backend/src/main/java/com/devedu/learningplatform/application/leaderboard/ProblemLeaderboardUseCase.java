package com.devedu.learningplatform.application.leaderboard;

import com.devedu.learningplatform.domain.leaderboard.ProblemLeaderboardPeriod;

public interface ProblemLeaderboardUseCase {
    ProblemLeaderboardView get(ProblemLeaderboardPeriod period);
}
