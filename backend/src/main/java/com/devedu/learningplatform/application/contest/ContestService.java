package com.devedu.learningplatform.application.contest;

import com.devedu.learningplatform.application.port.in.command.SubmitProblemCommand;
import com.devedu.learningplatform.application.port.out.ProgrammingProblemRepository;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.domain.contest.Contest;
import com.devedu.learningplatform.domain.model.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import static com.devedu.learningplatform.application.contest.ContestException.Kind.*;

public final class ContestService implements ContestUseCase {
    private final ContestRepository repository;
    private final ProgrammingProblemRepository problems;
    private final ContestSubmissionPort submissions;
    private final Clock clock;
    private final com.devedu.learningplatform.application.contest.scoring.ContestScoring scoring =
            new com.devedu.learningplatform.application.contest.scoring.ContestScoring();
    public ContestService(ContestRepository repository, ProgrammingProblemRepository problems,
                          ContestSubmissionPort submissions, Clock clock) {
        this.repository = repository; this.problems = problems; this.submissions = submissions; this.clock = clock;
    }

    @Override public ContestViews.Catalog list(Contest.Status status) {
        return list(status,null,null,"START_TIME");
    }
    @Override public ContestViews.Catalog list(Contest.Status status, Boolean rated, Contest.Difficulty difficulty, String sort) {
        if (!Set.of("START_TIME","PARTICIPANTS","POPULARITY").contains(sort)) throw new IllegalArgumentException("Invalid sort");
        var now = clock.instant();
        var contests = repository.search(status, now, rated, difficulty, sort);
        var counts = repository.participantCounts(contests.stream().map(Contest::id).toList());
        return new ContestViews.Catalog(contests.stream().map(c -> summary(c, now, counts.getOrDefault(c.id(), 0L))).toList(), now);
    }

    @Override public UUID create(Create command, AuthenticatedUser creator) {
        if (creator == null || (creator.role() != UserRole.TEACHER && creator.role() != UserRole.ADMIN)) {
            throw new ContestException(FORBIDDEN, "Only teachers and admins can create contests");
        }
        if (command == null) throw new IllegalArgumentException("Contest is required");
        var contest = new Contest(UUID.randomUUID(), command.name(), command.type(), command.startsAt(),
                command.durationMinutes(), creator.id(), command.rules(), command.problems(), command.rated(), command.difficulty());
        if (!contest.startsAt().isAfter(clock.instant())) throw new IllegalArgumentException("Start time must be in the future");
        var ids = contest.problems().stream().map(Contest.Problem::problemId).toList();
        if (problems.findAllByIds(ids).size() != ids.size()) throw new IllegalArgumentException("One or more problems are unavailable");
        return repository.save(contest).id();
    }

    @Override public void register(UUID id, AuthenticatedUser user) {
        requireUser(user);
        var contest = find(id);
        var now = clock.instant();
        if (contest.statusAt(now) == Contest.Status.FINISHED) throw new ContestException(CONFLICT, "Contest has ended");
        repository.register(id, user.id(), now);
    }

    @Override public ProblemSubmission submit(UUID id, UUID problemId, CodeLanguage language, String code, AuthenticatedUser user) {
        return submit(id, problemId, language, code, user, null);
    }
    @Override public ProblemSubmission submit(UUID id, UUID problemId, CodeLanguage language, String code, AuthenticatedUser user, String requestKey) {
        requireUser(user);
        var contest = find(id);
        var receivedAt = clock.instant();
        if (contest.statusAt(receivedAt) != Contest.Status.ONGOING) throw new ContestException(CONFLICT, "Contest is not accepting submissions");
        if (!repository.registered(id, user.id())) throw new ContestException(FORBIDDEN, "Register for this contest before submitting");
        if (contest.problems().stream().noneMatch(p -> p.problemId().equals(problemId))) throw new ContestException(NOT_FOUND, "Problem is not part of this contest");
        var problem = problems.findById(problemId).orElseThrow(() -> new ContestException(NOT_FOUND, "Problem is unavailable"));
        if (language == null || code == null || code.isBlank() || code.length() > 100000) throw new IllegalArgumentException("Language and code (up to 100000 characters) are required");
        var command = new SubmitProblemCommand(user.id(), problem.slug(), language, code);
        return requestKey == null ? submissions.submit(id, command, receivedAt) : submissions.submit(id, command, receivedAt, null, requestKey);
    }

    @Override public ContestViews.Detail detail(UUID id, AuthenticatedUser viewer) {
        var contest = find(id);
        var now = clock.instant();
        var people = repository.participants(id);
        var entries = repository.entries(id);
        var catalog = problems.findAllByIds(contest.problems().stream().map(Contest.Problem::problemId).toList())
                .stream().collect(Collectors.toMap(ProgrammingProblem::id, p -> p));
        var points = contest.problems().stream().collect(Collectors.toMap(Contest.Problem::problemId, Contest.Problem::points));
        var accepted = new HashMap<UUID, Map<UUID, Instant>>();
        for (var entry : entries) {
            if (entry.status() == SubmissionStatus.ACCEPTED) accepted.computeIfAbsent(entry.userId(), ignored -> new HashMap<>())
                    .merge(entry.problemId(), entry.receivedAt(), (a, b) -> a.isBefore(b) ? a : b);
        }
        var board = scoring.rank(contest, people, entries);
        var myParticipant = viewer == null ? Optional.<ContestRepository.Participant>empty()
                : people.stream().filter(p -> p.internalId().equals(viewer.id())).findFirst();
        var myResult = myParticipant.flatMap(p -> board.stream().filter(s -> s.userId() == p.publicId()).findFirst()).orElse(null);
        var rows = new ArrayList<ContestViews.Problem>();
        var counts = entries.stream().collect(Collectors.groupingBy(ContestRepository.Entry::problemId, Collectors.counting()));
        for (int i = 0; i < contest.problems().size(); i++) {
            var ref = contest.problems().get(i); var problem = catalog.get(ref.problemId());
            boolean solved = viewer != null && accepted.getOrDefault(viewer.id(), Map.of()).containsKey(ref.problemId());
            rows.add(new ContestViews.Problem(ref.problemId(), problem == null ? null : problem.slug(),
                    problem == null ? "Problem unavailable" : problem.title(), problem == null ? null : problem.difficulty(),
                    String.valueOf((char) ('A' + i)), ref.points(), solved, counts.getOrDefault(ref.problemId(), 0L), problem != null));
        }
        var rowMap = rows.stream().collect(Collectors.toMap(ContestViews.Problem::id, p -> p));
        var mine = entries.stream().filter(e -> viewer != null && e.userId().equals(viewer.id()))
                .sorted(Comparator.comparing(ContestRepository.Entry::receivedAt).reversed())
                .map(e -> new ContestViews.Submission(e.id(), e.problemId(), rowMap.get(e.problemId()).letter(),
                        rowMap.get(e.problemId()).title(), e.language(), e.status(), e.passedTests(), e.totalTests(),
                        e.executionTimeMillis(), e.receivedAt(), e.status() == SubmissionStatus.ACCEPTED ? points.get(e.problemId()) : 0,
                        points.get(e.problemId()))).toList();
        return new ContestViews.Detail(summary(contest, now, people.size()), now, myParticipant.isPresent(), contest.rules(), rows, board, myResult, mine, scoring.rules());
    }

    private Contest find(UUID id) { return repository.find(id).orElseThrow(() -> new ContestException(NOT_FOUND, "Contest not found")); }
    private void requireUser(AuthenticatedUser user) { if (user == null) throw new ContestException(FORBIDDEN, "Sign in to participate"); }
    private ContestViews.Summary summary(Contest c, Instant now, long count) {
        return new ContestViews.Summary(c.id(), c.name(), c.type(), c.startsAt(), c.endsAt(), c.durationMinutes(), c.problems().size(), count, c.statusAt(now), c.rated(), c.difficulty());
    }
}
