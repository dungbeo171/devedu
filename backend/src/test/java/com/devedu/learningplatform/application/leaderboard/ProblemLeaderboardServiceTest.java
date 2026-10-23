package com.devedu.learningplatform.application.leaderboard;

import com.devedu.learningplatform.domain.leaderboard.ProblemLeaderboardPeriod;
import com.devedu.learningplatform.domain.model.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProblemLeaderboardServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-23T02:00:00Z");

    @Test
    void scoresDifficultyAndOrdersByScoreSolvedAndCompletionTime() {
        ProblemLeaderboardQueryPort query = (start, end) -> List.of(
                candidate(3, "An", 0, 0, 1, "2026-10-22T03:00:00Z"),
                candidate(2, "Bình", 1, 1, 0, "2026-10-22T02:00:00Z"),
                candidate(1, "Chi", 3, 0, 0, "2026-10-22T01:00:00Z")
        );
        var service = new ProblemLeaderboardService(query, Clock.fixed(NOW, ZoneOffset.UTC));

        var result = service.get(ProblemLeaderboardPeriod.WEEK);

        assertEquals(3, result.entries().size());
        assertEquals("Chi", result.entries().get(0).name());
        assertEquals("Bình", result.entries().get(1).name());
        assertEquals("An", result.entries().get(2).name());
        assertEquals(300, result.entries().get(0).score());
        assertEquals(Instant.parse("2026-10-18T17:00:00Z"), result.startsAt());
        assertEquals(Instant.parse("2026-10-25T17:00:00Z"), result.endsAt());
    }

    @Test
    void exposesCalendarMonthAndYearWindowsInVietnamTime() {
        var service = new ProblemLeaderboardService((start, end) -> List.of(), Clock.fixed(NOW, ZoneOffset.UTC));

        assertEquals(Instant.parse("2026-09-30T17:00:00Z"), service.get(ProblemLeaderboardPeriod.MONTH).startsAt());
        assertEquals(Instant.parse("2026-10-31T17:00:00Z"), service.get(ProblemLeaderboardPeriod.MONTH).endsAt());
        assertEquals(Instant.parse("2025-12-31T17:00:00Z"), service.get(ProblemLeaderboardPeriod.YEAR).startsAt());
        assertEquals(Instant.parse("2026-12-31T17:00:00Z"), service.get(ProblemLeaderboardPeriod.YEAR).endsAt());
    }

    private static ProblemLeaderboardQueryPort.Candidate candidate(
            long id, String name, long easy, long medium, long hard, String lastAcceptedAt
    ) {
        return new ProblemLeaderboardQueryPort.Candidate(
                id, name, UserRole.STUDENT, easy, medium, hard, Instant.parse(lastAcceptedAt));
    }
}
