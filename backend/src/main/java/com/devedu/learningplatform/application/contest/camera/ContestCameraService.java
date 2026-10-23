package com.devedu.learningplatform.application.contest.camera;

import com.devedu.learningplatform.application.contest.ContestException;
import com.devedu.learningplatform.application.contest.ContestRepository;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.domain.contest.CameraEvent;
import com.devedu.learningplatform.domain.contest.Contest;
import com.devedu.learningplatform.domain.contest.ContestAttempt;
import com.devedu.learningplatform.domain.model.UserRole;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.UUID;
import static com.devedu.learningplatform.application.contest.ContestException.Kind.*;

public final class ContestCameraService implements ContestCameraUseCase {
    public static final int RETENTION_DAYS = 30;
    private static final int TIMELINE_PAGE_SIZE = 20;
    private static final EnumSet<CameraEvent.Kind> REPORTABLE_EVENTS = EnumSet.of(
            CameraEvent.Kind.LOOKING_AWAY, CameraEvent.Kind.NO_FACE, CameraEvent.Kind.TAB_HIDDEN,
            CameraEvent.Kind.PROBLEM_COPIED, CameraEvent.Kind.CONTENT_PASTED, CameraEvent.Kind.SCREENSHOT_ATTEMPT);
    private final ContestRepository contests;
    private final ContestCameraRepository events;
    private final Clock clock;
    public ContestCameraService(ContestRepository contests, ContestCameraRepository events, Clock clock) {
        this.contests = contests; this.events = events; this.clock = clock;
    }
    @Override public void record(UUID contestId, UUID eventId, CameraEvent.Kind kind, AuthenticatedUser user) {
        requireUser(user);
        var contest = find(contestId);
        var now = clock.instant();
        if (contest.statusAt(now) != Contest.Status.ONGOING) throw new ContestException(CONFLICT, "Camera events require a live official contest");
        if (!contests.registered(contestId, user.id())) throw new ContestException(FORBIDDEN, "Register before enabling the camera");
        if (!contest.monitoringEnabled()) throw new ContestException(CONFLICT, "Cuộc thi không bật chế độ giám sát.");
        var startedAt = contests.attemptStartedAt(contestId, user.id());
        if (startedAt.isEmpty() || !ContestAttempt.startingAt(contest, startedAt.get()).activeAt(now)) {
            throw new ContestException(CONFLICT, "Chỉ ghi nhận camera trong thời gian làm bài chính thức.");
        }
        events.append(new CameraEvent(eventId, contestId, user.id(), kind, now));
    }
    @Override public Mine mine(UUID contestId, AuthenticatedUser user) {
        requireUser(user);
        var contest = find(contestId);
        var now = clock.instant();
        var attempt = contests.attemptStartedAt(contestId, user.id());
        boolean registered = contests.registered(contestId, user.id());
        // Allow camera setup before starting the clock, but never after the personal deadline.
        boolean canMonitor = contest.monitoringEnabled() && contest.statusAt(now) == Contest.Status.ONGOING
                && registered
                && (attempt.isEmpty() || ContestAttempt.startingAt(contest, attempt.get()).activeAt(now));
        long lookingAwayCount = contest.monitoringEnabled() && registered
                ? events.count(contestId, user.id(), CameraEvent.Kind.LOOKING_AWAY,
                    now.minus(RETENTION_DAYS, ChronoUnit.DAYS))
                : 0;
        return new Mine(canReview(contest, user), canMonitor, lookingAwayCount, RETENTION_DAYS, now);
    }
    @Override public Report report(UUID contestId, int page, AuthenticatedUser user) {
        requireUser(user);
        if (!canReview(find(contestId), user)) throw new ContestException(FORBIDDEN, "Only the contest owner or an admin can view camera reports");
        if (page < 1 || page > 100000) throw new IllegalArgumentException("Invalid page");
        return new Report(events.summaries(contestId, (page - 1) * 20, 20, clock.instant().minus(RETENTION_DAYS, ChronoUnit.DAYS)),
                events.participantCount(contestId), page, 20, RETENTION_DAYS, clock.instant());
    }
    @Override public Timeline timeline(UUID contestId, long publicUserId, CameraEvent.Kind kind, int page, AuthenticatedUser user) {
        requireUser(user);
        if (!canReview(find(contestId), user)) throw new ContestException(FORBIDDEN, "Only authorized reviewers can view camera timelines");
        if (publicUserId < 1 || page < 1 || page > 100000 || kind == null || !REPORTABLE_EVENTS.contains(kind)) {
            throw new IllegalArgumentException("Invalid camera timeline request");
        }
        var since = clock.instant().minus(RETENTION_DAYS, ChronoUnit.DAYS);
        return new Timeline(publicUserId, kind,
                events.timeline(contestId, publicUserId, kind, (page - 1) * TIMELINE_PAGE_SIZE, TIMELINE_PAGE_SIZE, since),
                events.timelineCount(contestId, publicUserId, kind, since), page, TIMELINE_PAGE_SIZE, clock.instant());
    }
    private boolean canReview(Contest contest, AuthenticatedUser user) {
        return user.role() == UserRole.ADMIN || user.role() == UserRole.TEACHER
                && (contest.createdBy().equals(user.id()) || contests.isProctor(contest.id(), user.id()));
    }
    private Contest find(UUID id) { return contests.find(id).orElseThrow(() -> new ContestException(NOT_FOUND, "Contest not found")); }
    private void requireUser(AuthenticatedUser user) {
        if (user == null) throw new ContestException(FORBIDDEN, "Sign in to use contest camera features");
    }
}
