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
            UUID.randomUUID(), List.of(new Contest.Problem(problemId, 100)));
    private final ContestRepository repository = mock(ContestRepository.class);
    private final ProgrammingProblemRepository problems = mock(ProgrammingProblemRepository.class);
    private final ContestSubmissionPort submissions = mock(ContestSubmissionPort.class);
    private final AuthenticatedUser user = new AuthenticatedUser(userId, "student@example.com", UserRole.STUDENT);
    private final ProgrammingProblem fixture = problem();

    private ContestService service(Instant time) {
        when(repository.find(contestId)).thenReturn(Optional.of(contest));
        when(repository.attemptStartedAt(contestId, userId)).thenReturn(Optional.of(start));
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
        var command = new ContestUseCase.Create("Contest", Contest.Type.CUSTOM, start, 60,
                List.of(new Contest.Problem(problemId, 10)));
        var service = service(start.minusSeconds(60));
        assertThatThrownBy(() -> service.create(command, user)).isInstanceOf(ContestException.class);
        when(problems.findAllByIds(List.of(problemId))).thenReturn(List.of());
        assertThatThrownBy(() -> service.create(command, creator)).isInstanceOf(IllegalArgumentException.class);
        when(problems.findAllByIds(List.of(problemId))).thenReturn(List.of(fixture));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.create(command, creator)).isNotNull();
        verify(repository).save(argThat(c -> !c.published() && c.visibility() == Contest.Visibility.PRIVATE));
    }
    @Test void restrictedContestChecksAuthenticatedEmailBeforeRegistration() {
        var restricted = new Contest(contestId, "Restricted", Contest.Type.CUSTOM, start, 60, UUID.randomUUID(),
                contest.problems(), Contest.Difficulty.BEGINNER, contest.endsAt(), "", false, true,
                Contest.Visibility.PUBLIC, Contest.ParticipationMode.RESTRICTED);
        var service = service(start.minusSeconds(1));
        when(repository.find(contestId)).thenReturn(Optional.of(restricted));
        when(repository.emailAllowed(contestId, user.email())).thenReturn(false);
        assertThatThrownBy(() -> service.register(contestId, user))
                .isInstanceOf(ContestException.class).hasMessageContaining("Email");
        verify(repository, never()).register(any(), any(), any());
        when(repository.emailAllowed(contestId, user.email())).thenReturn(true);
        service.register(contestId, user);
        verify(repository).register(contestId, userId, start.minusSeconds(1));
    }
    @Test void catalogExposesParticipationModeWithoutExposingInternalCreatorId() {
        var restricted = new Contest(contestId, "Restricted", Contest.Type.CUSTOM, start, 60, UUID.randomUUID(),
                contest.problems(), Contest.Difficulty.BEGINNER, contest.endsAt(), "", false, true,
                Contest.Visibility.PUBLIC, Contest.ParticipationMode.RESTRICTED);
        when(repository.search(Contest.Status.UPCOMING, start.minusSeconds(1), null, "START_TIME"))
                .thenReturn(List.of(restricted));
        when(repository.participantCounts(List.of(contestId))).thenReturn(Map.of(contestId, 3L));
        var row = service(start.minusSeconds(1)).list(Contest.Status.UPCOMING, null, "START_TIME").contests().get(0);
        assertThat(row.participationMode()).isEqualTo(Contest.ParticipationMode.RESTRICTED);
        assertThat(row.participants()).isEqualTo(3);
    }
    @Test void publishingDraftRequiresProblemsAndAllowedEmailForRestrictedMode() {
        var creator = new AuthenticatedUser(userId, "teacher@example.com", UserRole.TEACHER);
        var draft = new Contest(contestId, "Draft", Contest.Type.CUSTOM, start, 60, userId, List.of(),
                Contest.Difficulty.BEGINNER, contest.endsAt(), "", false, false,
                Contest.Visibility.PRIVATE, Contest.ParticipationMode.OPEN);
        var service = service(start.minusSeconds(1));
        when(repository.find(contestId)).thenReturn(Optional.of(draft));
        assertThatThrownBy(() -> service.publish(contestId, creator))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("problem");
        var configured = new Contest(contestId, "Draft", Contest.Type.CUSTOM, start, 60, userId,
                List.of(new Contest.Problem(problemId, 10)),
                Contest.Difficulty.BEGINNER, contest.endsAt(), "", false, false,
                Contest.Visibility.PRIVATE, Contest.ParticipationMode.RESTRICTED);
        when(repository.find(contestId)).thenReturn(Optional.of(configured));
        when(repository.allowedEmails(contestId)).thenReturn(List.of());
        assertThatThrownBy(() -> service.publish(contestId, creator))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("email");
        when(repository.allowedEmails(contestId)).thenReturn(List.of("student@example.com"));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.publish(contestId, creator);
        verify(repository).save(argThat(Contest::published));
    }
    @Test void savingPublicSettingsPublishesAtomicallyAndRequiresTenTotalPoints() {
        var creator = new AuthenticatedUser(userId, "teacher@example.com", UserRole.TEACHER);
        var validProblems = List.of(new Contest.Problem(problemId, 10));
        var draft = new Contest(contestId, "Draft", Contest.Type.CUSTOM, start, 60, userId, validProblems,
                Contest.Difficulty.BEGINNER, contest.endsAt(), "", false, false,
                Contest.Visibility.PRIVATE, Contest.ParticipationMode.OPEN);
        var service = service(start.minusSeconds(1));
        when(repository.find(contestId)).thenReturn(Optional.of(draft));

        service.updateSettings(contestId, new ContestUseCase.UpdateSettings(
                Contest.Visibility.PUBLIC, Contest.ParticipationMode.OPEN, List.of(), List.of()), creator);

        verify(repository).saveConfiguration(argThat(saved -> saved.published()
                && saved.visibility() == Contest.Visibility.PUBLIC), eq(List.of()), eq(List.of()));

        assertThatThrownBy(() -> service.updateProblems(contestId,
                List.of(new Contest.Problem(problemId, 9)), creator))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("10 total points");
    }
    @Test void managementListContainsOnlyCreatorsContestsAndTreatsDraftAsPrivate() {
        var creator = new AuthenticatedUser(userId, "teacher@example.com", UserRole.TEACHER);
        var draft = new Contest(contestId, "Unfinished", Contest.Type.CUSTOM, start, 60, userId, List.of(),
                Contest.Difficulty.BEGINNER, contest.endsAt(), "Draft", false, false,
                Contest.Visibility.PUBLIC, Contest.ParticipationMode.OPEN);
        when(repository.ownedBy(userId)).thenReturn(List.of(draft));
        when(repository.participantCounts(List.of(contestId))).thenReturn(Map.of());
        var catalog = service(start.minusSeconds(10)).managed(creator);
        assertThat(catalog.contests()).singleElement().satisfies(row -> {
            assertThat(row.published()).isFalse();
            assertThat(row.visibility()).isEqualTo(Contest.Visibility.PRIVATE);
            assertThat(row.editable()).isTrue();
        });
        verify(repository).ownedBy(userId);
        verify(repository, never()).all();
        assertThatThrownBy(() -> service(start).managed(user)).isInstanceOf(ContestException.class);
    }
    @Test void adminManagementListsAllContestsWhileTeacherCannotManageAnotherOwnersContest() {
        var ownerId = UUID.randomUUID();
        var ownedBySomeoneElse = new Contest(contestId, "Other teacher", Contest.Type.CUSTOM, start, 60,
                ownerId, contest.problems());
        when(repository.all()).thenReturn(List.of(ownedBySomeoneElse));
        when(repository.participantCounts(List.of(contestId))).thenReturn(Map.of());
        var admin = new AuthenticatedUser(userId, "admin@example.com", UserRole.ADMIN);
        assertThat(service(start.minusSeconds(1)).managed(admin).contests()).hasSize(1);
        verify(repository).all();
        verify(repository, never()).ownedBy(userId);

        when(repository.find(contestId)).thenReturn(Optional.of(ownedBySomeoneElse));
        var teacher = new AuthenticatedUser(userId, "teacher@example.com", UserRole.TEACHER);
        var update = new ContestUseCase.UpdateDetails("Changed", Contest.Type.CUSTOM, start, contest.endsAt(),
                60, Contest.Difficulty.BEGINNER, "", false);
        assertThatThrownBy(() -> service(start.minusSeconds(1)).updateDetails(contestId, update, teacher))
                .isInstanceOf(ContestException.class).hasMessageContaining("owner");
        verify(repository, never()).save(argThat(saved -> saved.name().equals("Changed")));
    }
    @Test void teacherCanListAssignedContestsWithoutReceivingEditPermission() {
        var ownerId = UUID.randomUUID();
        var assigned = new Contest(contestId, "Assigned", Contest.Type.CUSTOM, start, 60,
                ownerId, contest.problems());
        var teacher = new AuthenticatedUser(userId, "teacher@example.com", UserRole.TEACHER);
        when(repository.supervisedBy(userId)).thenReturn(List.of(assigned));
        when(repository.participantCounts(List.of(contestId))).thenReturn(Map.of(contestId, 2L));

        var catalog = service(start.minusSeconds(1)).supervised(teacher);

        assertThat(catalog.contests()).singleElement().satisfies(row -> {
            assertThat(row.name()).isEqualTo("Assigned");
            assertThat(row.participants()).isEqualTo(2);
            assertThat(row.editable()).isFalse();
        });
        verify(repository).supervisedBy(userId);
        verify(repository, never()).ownedBy(userId);
        var update = new ContestUseCase.UpdateDetails("Changed", Contest.Type.CUSTOM, start, contest.endsAt(),
                60, Contest.Difficulty.BEGINNER, "", false);
        assertThatThrownBy(() -> service(start.minusSeconds(1)).updateDetails(contestId, update, teacher))
                .isInstanceOf(ContestException.class).hasMessageContaining("owner");
    }
    @Test void adminDoesNotNeedASeparateAssignedContestCatalog() {
        var admin = new AuthenticatedUser(userId, "admin@example.com", UserRole.ADMIN);

        assertThat(service(start.minusSeconds(1)).supervised(admin).contests()).isEmpty();

        verify(repository, never()).supervisedBy(any());
    }
    @Test void finishedContestsArePersistedAndPresentedAsPrivate() {
        var creator = new AuthenticatedUser(userId, "teacher@example.com", UserRole.TEACHER);
        var finished = new Contest(contestId, "Finished", Contest.Type.CUSTOM, start, 60, userId,
                contest.problems(), Contest.Difficulty.BEGINNER, start.plusSeconds(3600), "", false, true,
                Contest.Visibility.PUBLIC, Contest.ParticipationMode.OPEN);
        var now = start.plusSeconds(3600);
        when(repository.ownedBy(userId)).thenReturn(List.of(finished));
        when(repository.participantCounts(List.of(contestId))).thenReturn(Map.of());
        var row = service(now).managed(creator).contests().get(0);
        assertThat(row.status()).isEqualTo(Contest.Status.FINISHED);
        assertThat(row.visibility()).isEqualTo(Contest.Visibility.PRIVATE);
        assertThat(row.editable()).isTrue();
        verify(repository).privatizeFinished(now);
    }
    @Test void ownerCanEditOngoingAndFinishedContest() {
        var creator = new AuthenticatedUser(userId, "teacher@example.com", UserRole.TEACHER);
        var ongoing = new Contest(contestId, "Live", Contest.Type.CUSTOM, start, 60, userId,
                List.of(new Contest.Problem(problemId, 10)), Contest.Difficulty.BEGINNER,
                start.plusSeconds(3600), "", false, true, Contest.Visibility.PUBLIC,
                Contest.ParticipationMode.OPEN);
        var update = new ContestUseCase.UpdateDetails("Live updated", Contest.Type.CUSTOM, start,
                ongoing.endsAt(), 60, Contest.Difficulty.BEGINNER, "", false);
        var liveService = service(start.plusSeconds(60));
        when(repository.find(contestId)).thenReturn(Optional.of(ongoing));

        liveService.updateDetails(contestId, update, creator);
        verify(repository).save(argThat(saved -> saved.name().equals("Live updated")));

        var endedService = service(ongoing.endsAt());
        when(repository.find(contestId)).thenReturn(Optional.of(ongoing));
        endedService.updateDetails(contestId, update, creator);
        when(problems.findAllByIds(List.of(problemId))).thenReturn(List.of(fixture));
        endedService.updateProblems(contestId, List.of(new Contest.Problem(problemId, 10)), creator);
        endedService.updateSettings(contestId, new ContestUseCase.UpdateSettings(
                Contest.Visibility.PRIVATE, Contest.ParticipationMode.OPEN, List.of(), List.of()), creator);

        verify(repository, times(3)).save(argThat(saved -> saved.name().equals("Live updated")
                || saved.problems().equals(List.of(new Contest.Problem(problemId, 10)))));
        verify(repository).saveConfiguration(any(), eq(List.of()), eq(List.of()));
    }
    @Test void createsPastStartWithIndependentEndDescriptionAndDeferredMonitoring() {
        var service = service(start.plusSeconds(600));
        when(problems.findAllByIds(List.of(problemId))).thenReturn(List.of(fixture));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var command = new ContestUseCase.Create("Past start", Contest.Type.CUSTOM, start, 30,
                List.of(new Contest.Problem(problemId, 10)),
                Contest.Difficulty.BEGINNER, start.plusSeconds(14400), "  Description  ", true);
        service.create(command, new AuthenticatedUser(userId, "teacher@example.com", UserRole.TEACHER));
        verify(repository).save(argThat(c -> c.startsAt().equals(start) && c.endsAt().equals(start.plusSeconds(14400))
                && c.durationMinutes() == 30 && c.description().equals("Description") && c.monitoringEnabled()
                && c.createdAt().equals(start.plusSeconds(600))));
    }
    @Test void personalStartIsRequiredAndDeadlineIsExclusiveBeforeGlobalEnd() {
        var service = service(start.plusSeconds(1800));
        var extended = new Contest(contestId, "Window", Contest.Type.CUSTOM, start, 10, userId, contest.problems(),
                Contest.Difficulty.BEGINNER, start.plusSeconds(14400), "", false);
        when(repository.find(contestId)).thenReturn(Optional.of(extended));
        when(repository.registered(contestId, userId)).thenReturn(true);
        when(repository.attemptStartedAt(contestId, userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.submit(contestId, problemId, CodeLanguage.PYTHON, "print(1)", user)).isInstanceOf(ContestException.class);
        when(repository.attemptStartedAt(contestId, userId)).thenReturn(Optional.of(start.plusSeconds(1200)));
        assertThatThrownBy(() -> service.submit(contestId, problemId, CodeLanguage.PYTHON, "print(1)", user)).isInstanceOf(ContestException.class);
        verifyNoInteractions(submissions);
    }
    @Test void startUsesStoredClockAndDoesNotResetExistingAttempt() {
        var service = service(start.plusSeconds(100));
        when(repository.registered(contestId, userId)).thenReturn(true);
        when(repository.startAttempt(contestId, userId, start.plusSeconds(100))).thenReturn(start.plusSeconds(10));
        var attempt = service.startAttempt(contestId, user);
        assertThat(attempt.startsAt()).isEqualTo(start.plusSeconds(10));
        assertThat(attempt.endsAt()).isEqualTo(contest.endsAt());
        assertThat(service.startAttempt(contestId, user)).isEqualTo(attempt);
    }
    @Test void startRejectsUnregisteredUpcomingEndedAndAnonymousUsers() {
        for (var at : List.of(start.minusNanos(1), contest.endsAt())) {
            assertThatThrownBy(() -> service(at).startAttempt(contestId, user)).isInstanceOf(ContestException.class);
        }
        assertThatThrownBy(() -> service(start).startAttempt(contestId, user)).isInstanceOf(ContestException.class);
        assertThatThrownBy(() -> service(start).startAttempt(contestId, null)).isInstanceOf(ContestException.class);
        verify(repository, never()).startAttempt(any(), any(), any());
    }
    @Test void rejectsDuplicateProblemsAndInvalidPoints() {
        assertThatThrownBy(() -> new Contest(contestId, "Test", Contest.Type.CUSTOM, start, 60, userId,
                List.of(new Contest.Problem(problemId, 100), new Contest.Problem(problemId, 200))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Contest.Problem(problemId, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Contest(contestId, " ", Contest.Type.CUSTOM, start, 60, userId, contest.problems()))
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
