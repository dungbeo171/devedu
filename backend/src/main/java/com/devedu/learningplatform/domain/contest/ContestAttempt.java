package com.devedu.learningplatform.domain.contest;

import java.time.Instant;

/** An official participant's immutable clock, bounded by the contest window. */
public record ContestAttempt(Instant startsAt, Instant endsAt) {
    public ContestAttempt {
        if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("Thời gian làm bài không hợp lệ.");
        }
    }
    public static ContestAttempt startingAt(Contest contest, Instant startsAt) {
        var durationEnd = startsAt.plusSeconds(contest.durationMinutes() * 60L);
        return new ContestAttempt(startsAt, durationEnd.isBefore(contest.endsAt()) ? durationEnd : contest.endsAt());
    }
    public boolean activeAt(Instant now) { return !now.isBefore(startsAt) && now.isBefore(endsAt); }
}
