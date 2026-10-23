package com.devedu.learningplatform.application.contest;

import com.devedu.learningplatform.application.contest.camera.*;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.domain.contest.*;
import com.devedu.learningplatform.domain.model.UserRole;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ContestCameraServiceTest {
    private final Instant start = Instant.parse("2026-10-07T10:00:00Z");
    private final UUID id = UUID.randomUUID(), owner = UUID.randomUUID(), student = UUID.randomUUID();
    private final Contest contest = new Contest(id, "Camera test", Contest.Type.CUSTOM, start, 60, owner,
            List.of(new Contest.Problem(UUID.randomUUID(), 100)), Contest.Difficulty.BEGINNER,
            start.plusSeconds(7200), "", true);
    private final ContestRepository contests = mock(ContestRepository.class);
    private final ContestCameraRepository events = mock(ContestCameraRepository.class);
    private AuthenticatedUser user(UUID id, UserRole role) { return new AuthenticatedUser(id, "test@example.com", role); }
    private ContestCameraService service(Instant at) {
        when(contests.find(id)).thenReturn(Optional.of(contest));
        when(contests.attemptStartedAt(id, student)).thenReturn(Optional.of(start));
        return new ContestCameraService(contests, events, Clock.fixed(at, ZoneOffset.UTC));
    }
    @Test void onlyRegisteredUsersDuringOfficialWindowCanSend() {
        var user = user(student, UserRole.STUDENT);
        for (Instant at : List.of(start.minusNanos(1), contest.endsAt(), contest.endsAt().plusSeconds(1))) {
            assertThatThrownBy(() -> service(at).record(id, UUID.randomUUID(), CameraEvent.Kind.LOOKING_AWAY, user)).isInstanceOf(ContestException.class);
        }
        assertThatThrownBy(() -> service(start).record(id, UUID.randomUUID(), CameraEvent.Kind.LOOKING_AWAY, user)).isInstanceOf(ContestException.class);
        verifyNoInteractions(events);
    }
    @Test void authenticatesIdentityAndUsesServerTimeForAllRoles() {
        var service = service(start);
        when(contests.registered(id, student)).thenReturn(true);
        UUID eventId = UUID.randomUUID();
        for (UserRole role : UserRole.values()) service.record(id, eventId, CameraEvent.Kind.LOOKING_AWAY, user(student, role));
        verify(events, times(3)).append(new CameraEvent(eventId, id, student, CameraEvent.Kind.LOOKING_AWAY, start));
    }
    @Test void acceptsCameraHeartbeatWithoutChangingItsKind() {
        var service = service(start);
        when(contests.registered(id, student)).thenReturn(true);
        UUID eventId = UUID.randomUUID();
        service.record(id, eventId, CameraEvent.Kind.CAMERA_ACTIVE, user(student, UserRole.STUDENT));
        verify(events).append(new CameraEvent(eventId, id, student, CameraEvent.Kind.CAMERA_ACTIVE, start));
    }
    @Test void storesBrowserBehaviorSignalsWithoutChangingTheirKinds() {
        var service = service(start);
        when(contests.registered(id, student)).thenReturn(true);
        for (var kind : List.of(CameraEvent.Kind.TAB_HIDDEN, CameraEvent.Kind.PROBLEM_COPIED, CameraEvent.Kind.CONTENT_PASTED,
                CameraEvent.Kind.SCREENSHOT_ATTEMPT)) {
            var eventId = UUID.randomUUID();
            service.record(id, eventId, kind, user(student, UserRole.STUDENT));
            verify(events).append(new CameraEvent(eventId, id, student, kind, start));
        }
    }
    @Test void reviewerCanReadAValidatedServerTimestampTimeline() {
        var service = service(start);
        when(events.timeline(id, 7L, CameraEvent.Kind.CONTENT_PASTED, 20, 20, start.minus(30, java.time.temporal.ChronoUnit.DAYS)))
                .thenReturn(List.of(start.minusSeconds(5)));
        when(events.timelineCount(id, 7L, CameraEvent.Kind.CONTENT_PASTED, start.minus(30, java.time.temporal.ChronoUnit.DAYS))).thenReturn(21L);
        var result = service.timeline(id, 7L, CameraEvent.Kind.CONTENT_PASTED, 2, user(owner, UserRole.TEACHER));
        assertThat(result.occurrences()).containsExactly(start.minusSeconds(5));
        assertThat(result.total()).isEqualTo(21);
        assertThat(result.serverTime()).isEqualTo(start);
        assertThatThrownBy(() -> service.timeline(id, 7L, CameraEvent.Kind.CAMERA_ACTIVE, 1, user(owner, UserRole.TEACHER)))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejectsInvalidEventAndAnonymousUser() {
        var service = service(start);
        when(contests.registered(id, student)).thenReturn(true);
        assertThatThrownBy(() -> service.record(id, UUID.randomUUID(), CameraEvent.Kind.NO_FACE, null)).isInstanceOf(ContestException.class);
        assertThatThrownBy(() -> service.record(id, null, CameraEvent.Kind.NO_FACE, user(student, UserRole.STUDENT))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.record(id, UUID.randomUUID(), null, user(student, UserRole.STUDENT))).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(events);
    }
    @Test void onlyOwnerTeacherAndAdminCanSeeReportsIncludingAfterEnd() {
        var service = service(contest.endsAt());
        for (AuthenticatedUser denied : List.of(user(student, UserRole.STUDENT), user(student, UserRole.TEACHER), user(owner, UserRole.STUDENT))) {
            assertThatThrownBy(() -> service.report(id, 1, denied)).isInstanceOf(ContestException.class);
        }
        verifyNoInteractions(events);
        service.report(id, 1, user(owner, UserRole.TEACHER));
        service.report(id, 2, user(student, UserRole.ADMIN));
        var proctor = UUID.randomUUID();
        when(contests.isProctor(id, proctor)).thenReturn(true);
        service.report(id, 3, user(proctor, UserRole.TEACHER));
        verify(events).summaries(eq(id), eq(0), eq(20), any());
        verify(events).summaries(eq(id), eq(20), eq(20), any());
        verify(events).summaries(eq(id), eq(40), eq(20), any());
    }
    @Test void participantEndpointReturnsCapabilitiesAndOwnLookingAwayCount() {
        var service = service(start);
        when(contests.registered(id, student)).thenReturn(true);
        when(events.count(eq(id), eq(student), eq(CameraEvent.Kind.LOOKING_AWAY), any())).thenReturn(4L);
        var response = service.mine(id, user(student, UserRole.STUDENT));
        assertThat(response.canReview()).isFalse();
        assertThat(response.canMonitor()).isTrue();
        assertThat(response.lookingAwayCount()).isEqualTo(4);
        assertThat(response.serverTime()).isEqualTo(start);
        assertThat(service(contest.endsAt()).mine(id, user(student, UserRole.STUDENT)).canMonitor()).isFalse();
    }
    @Test void rejectsEventsBeforeAttemptAndAfterPersonalDeadlineEvenWhenContestIsOpen() {
        var service = service(start);
        when(contests.registered(id, student)).thenReturn(true);
        when(contests.attemptStartedAt(id, student)).thenReturn(Optional.empty());
        assertThat(service.mine(id, user(student, UserRole.STUDENT)).canMonitor()).isTrue();
        assertThatThrownBy(() -> service.record(id, UUID.randomUUID(), CameraEvent.Kind.LOOKING_AWAY, user(student, UserRole.STUDENT))).isInstanceOf(ContestException.class);
        var expired = service(start.plusSeconds(3600));
        assertThat(expired.mine(id, user(student, UserRole.STUDENT)).canMonitor()).isFalse();
        assertThatThrownBy(() -> expired.record(id, UUID.randomUUID(), CameraEvent.Kind.LOOKING_AWAY, user(student, UserRole.STUDENT))).isInstanceOf(ContestException.class);
        verify(events, never()).append(any());
    }
    @Test void unmonitoredContestNeverAcceptsCameraEvents() {
        var service = service(start);
        when(contests.registered(id, student)).thenReturn(true);
        when(contests.find(id)).thenReturn(Optional.of(new Contest(id, "No camera", Contest.Type.CUSTOM, start, 60, owner, contest.problems())));
        assertThat(service.mine(id, user(student, UserRole.STUDENT)).canMonitor()).isFalse();
        assertThatThrownBy(() -> service.record(id, UUID.randomUUID(), CameraEvent.Kind.LOOKING_AWAY, user(student, UserRole.STUDENT))).isInstanceOf(ContestException.class);
        verifyNoInteractions(events);
    }
    @Test void paginationIsBoundedAndMissingContestsAreRejected() {
        var service = service(start);
        for (int page : List.of(0, -1, 100001)) {
            assertThatThrownBy(() -> service.report(id, page, user(owner, UserRole.TEACHER))).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> service.mine(UUID.randomUUID(), user(student, UserRole.STUDENT))).isInstanceOf(ContestException.class);
        verifyNoInteractions(events);
    }
}
