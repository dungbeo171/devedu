package com.devedu.learningplatform.domain.leaderboard;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

public enum ProblemLeaderboardPeriod {
    WEEK,
    MONTH,
    YEAR;

    public Window window(Instant now, ZoneId zoneId) {
        var current = now.atZone(zoneId);
        var startDate = switch (this) {
            case WEEK -> current.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> current.toLocalDate().withDayOfMonth(1);
            case YEAR -> current.toLocalDate().withDayOfYear(1);
        };
        var start = startDate.atStartOfDay(zoneId);
        var end = switch (this) {
            case WEEK -> start.plusWeeks(1);
            case MONTH -> start.plusMonths(1);
            case YEAR -> start.plusYears(1);
        };
        return new Window(start.toInstant(), end.toInstant());
    }

    public record Window(Instant startsAt, Instant endsAt) {
        public Window {
            if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt)) {
                throw new IllegalArgumentException("Khoảng thời gian xếp hạng không hợp lệ");
            }
        }
    }
}
