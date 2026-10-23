package com.devedu.learningplatform.application.contest;

import com.devedu.learningplatform.domain.contest.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class ContestAttemptTest {
    private final Instant start = Instant.parse("2026-10-07T01:00:00Z");
    private Contest contest(Instant end, String description) {
        return new Contest(UUID.randomUUID(), "Window", Contest.Type.CUSTOM, start, 60, UUID.randomUUID(),
                List.of(new Contest.Problem(UUID.randomUUID(), 100)), Contest.Difficulty.BEGINNER, end, description, false);
    }
    @Test void personalTimerStartsOnEntryAndIsCappedAtGlobalEnd() {
        var c = contest(start.plusSeconds(4 * 3600), "");
        var early = ContestAttempt.startingAt(c, start.plusSeconds(3600));
        assertThat(early.endsAt()).isEqualTo(start.plusSeconds(7200));
        var late = ContestAttempt.startingAt(c, start.plusSeconds(3 * 3600 + 1800));
        assertThat(late.endsAt()).isEqualTo(c.endsAt());
        assertThat(late.activeAt(late.startsAt())).isTrue();
        assertThat(late.activeAt(late.endsAt().minusNanos(1))).isTrue();
        assertThat(late.activeAt(late.endsAt())).isFalse();
    }
    @Test void endAndDescriptionAreValidatedWithoutForbiddingPastDates() {
        assertThat(contest(start.plusSeconds(3600), "x".repeat(5000))).isNotNull();
        assertThatThrownBy(() -> contest(start, "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> contest(start.minusSeconds(1), "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> contest(start.plusSeconds(3600), "x".repeat(5001))).isInstanceOf(IllegalArgumentException.class);
    }
}
