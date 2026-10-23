package com.devedu.learningplatform.domain.leaderboard;

public final class ProblemLeaderboardScore {
    public static final int EASY_POINTS = 100;
    public static final int MEDIUM_POINTS = 200;
    public static final int HARD_POINTS = 300;

    private ProblemLeaderboardScore() {
    }

    public static long calculate(long easySolved, long mediumSolved, long hardSolved) {
        if (easySolved < 0 || mediumSolved < 0 || hardSolved < 0) {
            throw new IllegalArgumentException("Số bài đã giải không được âm");
        }
        return easySolved * EASY_POINTS + mediumSolved * MEDIUM_POINTS + hardSolved * HARD_POINTS;
    }
}
