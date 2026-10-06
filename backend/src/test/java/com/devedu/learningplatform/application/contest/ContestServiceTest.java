package com.devedu.learningplatform.application.contest;

import com.devedu.learningplatform.application.port.out.ProgrammingProblemRepository;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.domain.contest.Contest;
import com.devedu.learningplatform.domain.model.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ContestServiceTest {
    private final Instant start = Instant.parse("2026-11-01T10:00:00Z");
    private final UUID contestId = UUID.randomUUID(), problemId = UUID.randomUUID(), userId = UUID.randomUUID();
    private final Contest contest = new Contest(contestId, "Weekly", Contest.Type.WEEKLY, start, 60,
            UUID.randomUUID(), "", List.of(new Contest.Problem(problemId, 100)));
    private final ContestRepository repository = mock(ContestRepository.class);
    private final ProgrammingProblemRepository problems = mock(ProgrammingProblemRepository.class);
    private final ContestSubmissionPort submissions = mock(ContestSubmissionPort.class);
    private final AuthenticatedUser user = new AuthenticatedUser(userId, "student@example.com", UserRole.STUDENT);
    private final ProgrammingProblem fixture = problem();

    private ContestService service(Instant time) {
        when(repository.find(contestId)).thenReturn(Optional.of(contest));
        return new ContestService(repository, problems, submissions, Clock.fixed(time, ZoneOffset.UTC));
    }
    private ProgrammingProblem problem() {
        var problem = mock(ProgrammingProblem.class);
        when(problem.id()).thenReturn(problemId);
        when(problem.slug()).thenReturn("tong-hai-so");
        when(problem.title()).thenReturn("Tổng hai số");
        when(problem.difficulty()).thenReturn(ProblemDifficulty.EASY);
        return problem;
    }
    @Test void scheduleHasExactStartAndExclusiveEnd() {
        assertThat(contest.statusAt(start.minusNanos(1))).isEqualTo(Contest.Status.UPCOMING);
        assertThat(contest.statusAt(start)).isEqualTo(Contest.Status.ONGOING);
        assertThat(contest.statusAt(contest.endsAt().minusNanos(1))).isEqualTo(Contest.Status.ONGOING);
        assertThat(contest.statusAt(contest.endsAt())).isEqualTo(Contest.Status.FINISHED);
    }
    @Test void rejectsBeforeStartAndAtDeadlineWithoutCallingJudge() {
        for (var time : List.of(start.minusSeconds(1), contest.endsAt(), contest.endsAt().plusSeconds(1))) {
            assertThatThrownBy(() -> service(time).submit(contestId, problemId, CodeLanguage.PYTHON, "print(1)", user))
                    .isInstanceOf(ContestException.class).hasMessageContaining("not accepting");
        }
        verifyNoInteractions(submissions);
    }
    @Test void requiresRegistrationAndMembership() {
        var service = service(start);
        assertThatThrownBy(() -> service.submit(contestId, problemId, CodeLanguage.PYTHON, "print(1)", user))
                .isInstanceOf(ContestException.class).hasMessageContaining("Register");
        when(repository.registered(contestId, userId)).thenReturn(true);
        assertThatThrownBy(() -> service.submit(contestId, UUID.randomUUID(), CodeLanguage.PYTHON, "print(1)", user))
                .isInstanceOf(ContestException.class).hasMessageContaining("not part");
        verifyNoInteractions(submissions);
    }
    @Test void delegatesToExistingSubmissionFlowWithServerReceiptTimeForAllRoles() {
        var receivedAt = start.plusSeconds(1);
        var service = service(receivedAt);
        when(repository.registered(contestId, userId)).thenReturn(true);
        when(problems.findById(problemId)).thenReturn(Optional.of(fixture));
        for (var role : UserRole.values()) {
            service.submit(contestId, problemId, CodeLanguage.PYTHON, "print(1)",
                    new AuthenticatedUser(userId, "user@example.com", role));
        }
        verify(submissions, times(3)).submit(eq(contestId), argThat(c -> c.problemSlug().equals("tong-hai-so")
                && c.studentId().equals(userId)), eq(receivedAt));
    }
    @Test void rejectsDeletedProblemAndInvalidCode() {
        var service = service(start);
        when(repository.registered(contestId, userId)).thenReturn(true);
        assertThatThrownBy(() -> service.submit(contestId, problemId, CodeLanguage.PYTHON, "print(1)", user))
                .isInstanceOf(ContestException.class).hasMessageContaining("unavailable");
        when(problems.findById(problemId)).thenReturn(Optional.of(fixture));
        assertThatThrownBy(() -> service.submit(contestId, problemId, CodeLanguage.PYTHON, " ", user))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(submissions);
    }
    @Test void registersUntilEndButNotAfter() {
        service(start.minusSeconds(1)).register(contestId, user);
        verify(repository).register(contestId, userId, start.minusSeconds(1));
        assertThatThrownBy(() -> service(contest.endsAt()).register(contestId, user))
                .isInstanceOf(ContestException.class);
    }
    @Test void onlyTeachersAndAdminsCanCreateWithExistingProblems() {
        var creator = new AuthenticatedUser(userId, "teacher@example.com", UserRole.TEACHER);
        var command = new ContestUseCase.Create("Contest", Contest.Type.CUSTOM, start, 60, "", contest.problems());
        var service = service(start.minusSeconds(60));
        assertThatThrownBy(() -> service.create(command, user)).isInstanceOf(ContestException.class);
        when(problems.findAllByIds(List.of(problemId))).thenReturn(List.of());
        assertThatThrownBy(() -> service.create(command, creator)).isInstanceOf(IllegalArgumentException.class);
        when(problems.findAllByIds(List.of(problemId))).thenReturn(List.of(fixture));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.create(command, creator)).isNotNull();
    }
    @Test void rejectsDuplicateProblemsAndInvalidPoints() {
        assertThatThrownBy(() -> new Contest(contestId, "Test", Contest.Type.CUSTOM, start, 60, userId, "",
                List.of(new Contest.Problem(problemId, 100), new Contest.Problem(problemId, 200))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Contest.Problem(problemId, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Contest(contestId, " ", Contest.Type.CUSTOM, start, 60, userId, "", contest.problems()))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void computesOnlyContestScoresDeduplicatesAcceptedAndKeepsHistoryPrivate() {
        var other = UUID.randomUUID();
        var service = service(start.plusSeconds(500));
        when(repository.participants(contestId)).thenReturn(List.of(
                new ContestRepository.Participant(userId, 1, "Student"),
                new ContestRepository.Participant(other, 2, "Other")));
        when(problems.findAllByIds(List.of(problemId))).thenReturn(List.of(fixture));
        when(repository.entries(contestId)).thenReturn(List.of(
                entry(userId, SubmissionStatus.ACCEPTED, 100), entry(userId, SubmissionStatus.ACCEPTED, 200),
                entry(other, SubmissionStatus.WRONG_ANSWER, 10), entry(other, SubmissionStatus.ACCEPTED, 50)));
        var detail = service.detail(contestId, user);
        assertThat(detail.myResult().score()).isEqualTo(100);
        assertThat(detail.myResult().rank()).isEqualTo(2);
        assertThat(detail.myResult().timeSeconds()).isEqualTo(100);
        assertThat(detail.problems().get(0).solved()).isTrue();
        assertThat(detail.problems().get(0).submissions()).isEqualTo(4);
        assertThat(detail.mySubmissions()).hasSize(2);
        var anonymous = service.detail(contestId, null);
        assertThat(anonymous.mySubmissions()).isEmpty();
        assertThat(anonymous.registered()).isFalse();
        assertThat(anonymous.problems().get(0).solved()).isFalse();
    }
    private ContestRepository.Entry entry(UUID student, SubmissionStatus status, int seconds) {
        return new ContestRepository.Entry(UUID.randomUUID(), student, problemId, CodeLanguage.PYTHON, status,
                status == SubmissionStatus.ACCEPTED ? 3 : 0, 3, 100, start.plusSeconds(seconds));
    }
}
