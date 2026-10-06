package com.devedu.learningplatform.application.contest;

import com.devedu.learningplatform.application.contest.rating.*;
import com.devedu.learningplatform.application.contest.scoring.ContestScoring;
import com.devedu.learningplatform.application.port.out.*;
import com.devedu.learningplatform.application.port.in.command.SubmitProblemCommand;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.domain.contest.*;
import com.devedu.learningplatform.domain.model.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import static com.devedu.learningplatform.application.contest.ContestException.Kind.*;

public class ContestExperienceService implements ContestExperienceUseCase {
    private final ContestRepository contests;
    private final ContestExperienceRepository storage;
    private final ContestUseCase catalog;
    private final UserRepository users;
    private final ProgrammingProblemRepository problems;
    private final ContestSubmissionPort submissions;
    private final Clock clock;
    private final RatingCalculator rating;
    private final ContestScoring scoring = new ContestScoring();
    public ContestExperienceService(ContestRepository contests, ContestExperienceRepository storage, ContestUseCase catalog,
            UserRepository users, ProgrammingProblemRepository problems, ContestSubmissionPort submissions, Clock clock, RatingCalculator rating) {
        this.contests=contests; this.storage=storage; this.catalog=catalog; this.users=users; this.problems=problems;
        this.submissions=submissions; this.clock=clock; this.rating=rating;
    }
    @Override public void finalizeEnded() {
        if (!storage.lockFinalization()) return;
        for (var id : storage.awaitingFinalization(clock.instant())) {
            // Preserve end-time order; wait for any already-admitted judge request on the next tick.
            if (!storage.lockContestForFinalization(id)) break;
            if (storage.finalSnapshot(id).isPresent()) continue;
            var contest = find(id);
            var entries = contests.entries(id);
            var activeIds = entries.stream().map(ContestRepository.Entry::userId).collect(Collectors.toSet());
            var people = contests.participants(id).stream().filter(p -> activeIds.contains(p.internalId())).toList();
            var board = scoring.rank(contest, people, entries);
            var detail = catalog.detail(id, null);
            var snapshot = new ContestViews.Detail(detail.contest(), clock.instant(), false, detail.rules(), detail.problems(), board, null, List.of(), detail.scoringRules());
            var current = storage.ratings(people.stream().map(ContestRepository.Participant::publicId).toList());
            var changes = contest.rated() ? rating.calculateRatingChange(board.stream().map(s -> new RatingCalculator.Participant(
                    s.userId(), current.getOrDefault(s.userId(), EloRatingCalculator.INITIAL_RATING), s.score(), s.timeSeconds())).toList()) : List.<RatingCalculator.Change>of();
            var changeMap = changes.stream().collect(Collectors.toMap(RatingCalculator.Change::userId, c -> c));
            var userMap = people.stream().collect(Collectors.toMap(ContestRepository.Participant::publicId, ContestRepository.Participant::internalId));
            storage.saveSnapshot(id, snapshot, clock.instant());
            for (var row : board) storage.saveResult(id, userMap.get(row.userId()), row, changeMap.get(row.userId()), clock.instant());
        }
    }
    @Override public ContestExperienceViews.Result results(UUID id, AuthenticatedUser viewer) {
        var base = catalog.detail(id, viewer);
        var saved = storage.finalSnapshot(id);
        var detail = base;
        if (saved.isPresent()) {
            var snapshot = saved.get();
            var person = viewer == null ? null : users.findById(viewer.id()).orElse(null);
            var mine = person == null ? null : snapshot.leaderboard().stream().filter(s -> s.userId() == person.publicId()).findFirst().orElse(null);
            detail = new ContestViews.Detail(snapshot.contest(), clock.instant(), base.registered(), snapshot.rules(), snapshot.problems(),
                    snapshot.leaderboard(), mine, base.mySubmissions(), snapshot.scoringRules());
        }
        var mine = detail.myResult();
        int wrong = mine == null ? 0 : mine.problems().stream().mapToInt(ContestViews.ProblemScore::wrongAttempts).sum();
        double average = mine == null ? 0 : mine.problems().stream().filter(ContestViews.ProblemScore::solved)
                .mapToLong(ContestViews.ProblemScore::solvedAtSeconds).average().orElse(0);
        return new ContestExperienceViews.Result(detail, clock.instant(), saved.isPresent(), viewer == null ? null : storage.ratingChange(id, viewer.id()).orElse(null), wrong, average);
    }
    @Override public ContestExperienceViews.Profile profile(long publicId, AuthenticatedUser viewer) {
        var user = users.findByPublicId(publicId).orElseThrow(() -> new ContestException(NOT_FOUND, "User not found"));
        var history = storage.history(user.id());
        var virtuals = storage.virtualHistory(user.id());
        int current = storage.ratings(List.of(publicId)).getOrDefault(publicId, EloRatingCalculator.INITIAL_RATING);
        var last = history.stream().map(ContestExperienceViews.History::rating).filter(Objects::nonNull).findFirst().orElse(null);
        int peak = history.stream().filter(h -> h.rating() != null).mapToInt(h -> h.rating().newRating()).max().orElse(EloRatingCalculator.INITIAL_RATING);
        return new ContestExperienceViews.Profile(publicId, user.name(), "user-" + publicId, clock.instant(), current,
                last == null ? current : last.previousRating(), last == null ? 0 : last.ratingChange(), Math.max(peak, EloRatingCalculator.INITIAL_RATING),
                history.size(), (int) history.stream().filter(h -> h.rank()==1).count(), (int) history.stream().filter(h -> h.rank()<=10).count(),
                (int) history.stream().filter(h -> h.rank()<=100).count(), history.stream().mapToInt(ContestExperienceViews.History::rank).min().stream().boxed().findFirst().orElse(null),
                history.stream().mapToInt(ContestExperienceViews.History::rank).average().orElse(0), virtuals.size(), history,
                viewer != null && viewer.id().equals(user.id()) ? virtuals : List.of());
    }
    @Override public ContestExperienceViews.VirtualSession startVirtual(UUID id, AuthenticatedUser user) {
        requireUser(user); var contest=find(id);
        if (contest.statusAt(clock.instant()) != Contest.Status.FINISHED) throw new ContestException(CONFLICT,"Virtual is available only after the official contest ends");
        storage.lockVirtualStart(id,user.id());
        var session=storage.latestVirtual(id,user.id()).filter(v -> v.activeAt(clock.instant())).orElseGet(() -> {
            var now=clock.instant(); return storage.saveVirtual(new VirtualContest(UUID.randomUUID(),id,user.id(),now,now.plusSeconds(contest.durationMinutes()*60L)));
        });
        return virtualDetail(id,session.id(),user);
    }
    @Override public ContestExperienceViews.VirtualSession virtualDetail(UUID id, UUID sessionId, AuthenticatedUser user) {
        requireUser(user); var contest=find(id);
        var session=(sessionId == null ? storage.latestVirtual(id,user.id()) : storage.virtual(sessionId,user.id()))
                .filter(v -> v.contestId().equals(id)).orElseThrow(() -> new ContestException(NOT_FOUND,"No virtual attempt yet"));
        var person=users.findById(user.id()).orElseThrow();
        var entries=storage.virtualEntries(session.id());
        var virtualContest = new Contest(id,contest.name(),contest.type(),session.startsAt(),contest.durationMinutes(),contest.createdBy(),contest.rules(),contest.problems());
        var board=scoring.rank(virtualContest,List.of(new ContestRepository.Participant(user.id(),person.publicId(),person.name())),entries);
        var mine=board.get(0);
        var base=catalog.detail(id,null);
        var rows=base.problems().stream().map(p -> new ContestViews.Problem(p.id(),p.slug(),p.title(),p.difficulty(),p.letter(),p.points(),
                mine.problems().stream().anyMatch(s -> s.problemId().equals(p.id()) && s.solved()),entries.stream().filter(e -> e.problemId().equals(p.id())).count(),p.available())).toList();
        var rowMap=rows.stream().collect(Collectors.toMap(ContestViews.Problem::id,p -> p));
        var history=entries.stream().sorted(Comparator.comparing(ContestRepository.Entry::receivedAt).reversed()).map(e -> {
            var p=rowMap.get(e.problemId()); return new ContestViews.Submission(e.id(),p.id(),p.letter(),p.title(),e.language(),e.status(),e.passedTests(),e.totalTests(),e.executionTimeMillis(),e.receivedAt(),e.status()==SubmissionStatus.ACCEPTED?p.points():0,p.points());
        }).toList();
        var summary=new ContestViews.Summary(id,contest.name(),contest.type(),session.startsAt(),session.endsAt(),contest.durationMinutes(),rows.size(),1,session.activeAt(clock.instant())?Contest.Status.ONGOING:Contest.Status.FINISHED,false,contest.difficulty());
        var detail=new ContestViews.Detail(summary,clock.instant(),true,contest.rules(),rows,board,mine,history,scoring.rules());
        return new ContestExperienceViews.VirtualSession(new ContestExperienceViews.Session(session.id(),id,session.startsAt(),session.endsAt()),detail,clock.instant());
    }
    @Override public ProblemSubmission submitVirtual(UUID id, UUID sessionId, UUID problemId, CodeLanguage language, String code, String requestKey, AuthenticatedUser user) {
        requireUser(user); var contest=find(id);
        var session=storage.virtual(sessionId,user.id()).filter(v -> v.contestId().equals(id)).orElseThrow(() -> new ContestException(NOT_FOUND,"Virtual attempt not found"));
        if (!session.activeAt(clock.instant())) throw new ContestException(CONFLICT,"Virtual contest has ended");
        if (contest.problems().stream().noneMatch(p -> p.problemId().equals(problemId))) throw new ContestException(NOT_FOUND,"Problem is not part of this contest");
        var problem=problems.findById(problemId).orElseThrow(() -> new ContestException(NOT_FOUND,"Problem unavailable"));
        if (language==null || code==null || code.isBlank() || code.length()>100000) throw new IllegalArgumentException("Invalid code or language");
        return submissions.submit(id,new SubmitProblemCommand(user.id(),problem.slug(),language,code),clock.instant(),session.id(),requestKey);
    }
    private Contest find(UUID id) { return contests.find(id).orElseThrow(() -> new ContestException(NOT_FOUND,"Contest not found")); }
    private void requireUser(AuthenticatedUser user) { if (user==null) throw new ContestException(FORBIDDEN,"Sign in to participate"); }
}
