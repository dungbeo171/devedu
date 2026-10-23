package com.devedu.learningplatform.application.leaderboard;

import com.devedu.learningplatform.domain.leaderboard.ProblemLeaderboardPeriod;
import com.devedu.learningplatform.domain.leaderboard.ProblemLeaderboardScore;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Objects;

public final class ProblemLeaderboardService implements ProblemLeaderboardUseCase {
    static final ZoneId RANKING_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int MAX_ENTRIES = 100;

    private final ProblemLeaderboardQueryPort queryPort;
    private final Clock clock;

    public ProblemLeaderboardService(ProblemLeaderboardQueryPort queryPort, Clock clock) {
        this.queryPort = Objects.requireNonNull(queryPort);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public ProblemLeaderboardView get(ProblemLeaderboardPeriod period) {
        Objects.requireNonNull(period, "Kỳ xếp hạng là bắt buộc");
        var generatedAt = clock.instant();
        var window = period.window(generatedAt, RANKING_ZONE);
        var ranked = queryPort.findFirstAcceptedSolves(window.startsAt(), window.endsAt()).stream()
                .map(candidate -> new RankedCandidate(
                        candidate,
                        ProblemLeaderboardScore.calculate(
                                candidate.easySolved(), candidate.mediumSolved(), candidate.hardSolved()),
                        candidate.easySolved() + candidate.mediumSolved() + candidate.hardSolved()))
                .sorted(Comparator.comparingLong(RankedCandidate::score).reversed()
                        .thenComparing(Comparator.comparingLong(RankedCandidate::solved).reversed())
                        .thenComparing(item -> item.candidate().lastAcceptedAt())
                        .thenComparingLong(item -> item.candidate().userId()))
                .limit(MAX_ENTRIES)
                .toList();

        var entries = java.util.stream.IntStream.range(0, ranked.size())
                .mapToObj(index -> {
                    var item = ranked.get(index);
                    var candidate = item.candidate();
                    return new ProblemLeaderboardView.Entry(
                            index + 1, candidate.userId(), candidate.name(), candidate.role(),
                            item.score(), item.solved(), candidate.easySolved(),
                            candidate.mediumSolved(), candidate.hardSolved());
                })
                .toList();

        return new ProblemLeaderboardView(
                period, window.startsAt(), window.endsAt(), generatedAt,
                ProblemLeaderboardScore.EASY_POINTS,
                ProblemLeaderboardScore.MEDIUM_POINTS,
                ProblemLeaderboardScore.HARD_POINTS,
                entries);
    }

    private record RankedCandidate(ProblemLeaderboardQueryPort.Candidate candidate, long score, long solved) {
    }
}
