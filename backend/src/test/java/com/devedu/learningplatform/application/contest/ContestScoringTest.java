package com.devedu.learningplatform.application.contest;

import com.devedu.learningplatform.application.contest.scoring.ContestScoring;
import com.devedu.learningplatform.domain.contest.Contest;
import com.devedu.learningplatform.domain.model.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ContestScoringTest {
    private final UUID user = UUID.randomUUID(), first = UUID.randomUUID(), second = UUID.randomUUID();
    private final Instant start = Instant.parse("2026-10-01T00:00:00Z");
    private final ContestScoring scoring = new ContestScoring();
    private final Contest contest = new Contest(UUID.randomUUID(), "Test", Contest.Type.CUSTOM, start, 60, user, "",
            List.of(new Contest.Problem(first, 100), new Contest.Problem(second, 200)));
    private ContestRepository.Entry entry(UUID who, UUID problem, SubmissionStatus status, int seconds) {
        return new ContestRepository.Entry(UUID.randomUUID(), who, problem, CodeLanguage.CPP, status, 0, 3, 10, start.plusSeconds(seconds));
    }
    @Test void countsWrongAttemptsBeforeFirstAcceptedWithoutAddingPenaltyOrDuplicatePoints() {
        var rows = scoring.rank(contest, List.of(new ContestRepository.Participant(user, 1, "One")), List.of(
                entry(user, first, SubmissionStatus.ACCEPTED, 120), entry(user, first, SubmissionStatus.WRONG_ANSWER, 130),
                entry(user, first, SubmissionStatus.ACCEPTED, 150), entry(user, first, SubmissionStatus.COMPILE_ERROR, 10),
                entry(user, first, SubmissionStatus.WRONG_ANSWER, 30), entry(user, second, SubmissionStatus.TIME_LIMIT, 60)));
        var row = rows.get(0);
        assertThat(row.score()).isEqualTo(100);
        assertThat(row.solved()).isEqualTo(1);
        assertThat(row.timeSeconds()).isEqualTo(120);
        assertThat(row.problems().get(0).wrongAttempts()).isEqualTo(2);
        assertThat(row.problems().get(0).attempts()).isEqualTo(5);
        assertThat(row.problems().get(1).wrongAttempts()).isEqualTo(1);
        assertThat(row.problems().get(1).solvedAtSeconds()).isNull();
        assertThat(scoring.rules().wrongAttemptPenaltySeconds()).isZero();
        assertThat(scoring.rules().maxAttempts()).isNull();
        assertThat(scoring.rules().resubmissionAllowed()).isTrue();
    }
    @Test void breaksTiesByLastSolvedTimeThenPublicIdAndIncludesUnattemptedCells() {
        var other = UUID.randomUUID();
        var rows = scoring.rank(contest, List.of(new ContestRepository.Participant(user, 2, "Two"),
                new ContestRepository.Participant(other, 1, "One")), List.of(
                entry(user, second, SubmissionStatus.ACCEPTED, 100), entry(other, second, SubmissionStatus.ACCEPTED, 100)));
        assertThat(rows.get(0).userId()).isEqualTo(1);
        assertThat(rows.get(1).rank()).isEqualTo(2);
        assertThat(rows.get(0).problems().get(0).attempts()).isZero();
        assertThat(rows.get(0).problems().get(0).score()).isZero();
    }
}
