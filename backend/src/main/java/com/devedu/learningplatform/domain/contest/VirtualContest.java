package com.devedu.learningplatform.domain.contest;

import java.time.Instant;
import java.util.UUID;

public record VirtualContest(UUID id, UUID contestId, UUID userId, Instant startsAt, Instant endsAt) {
    public boolean activeAt(Instant now) { return !now.isBefore(startsAt) && now.isBefore(endsAt); }
}
