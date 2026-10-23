package com.devedu.learningplatform.application.contest;

import com.devedu.learningplatform.application.port.in.command.SubmitProblemCommand;
import com.devedu.learningplatform.application.port.in.command.CreateProgrammingProblemCommand;
import com.devedu.learningplatform.application.port.in.command.JudgeSubmissionCommand;
import com.devedu.learningplatform.application.port.in.CodeJudgeUseCase;
import com.devedu.learningplatform.application.port.in.result.JudgeResult;
import com.devedu.learningplatform.application.port.out.ProgrammingProblemRepository;
import com.devedu.learningplatform.application.port.out.UserRepository;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.domain.contest.Contest;
import com.devedu.learningplatform.domain.contest.ContestAttempt;
import com.devedu.learningplatform.domain.model.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import static com.devedu.learningplatform.application.contest.ContestException.Kind.*;

public final class ContestService implements ContestUseCase {
    private static final ContestProblemRepository NO_CONTEST_PROBLEMS = new ContestProblemRepository() {
        @Override public com.devedu.learningplatform.domain.contest.ContestProblemDefinition save(com.devedu.learningplatform.domain.contest.ContestProblemDefinition value) {
            throw new IllegalStateException("Contest problem repository is required");
        }
        @Override public Optional<com.devedu.learningplatform.domain.contest.ContestProblemDefinition> find(UUID contestId, UUID problemId) { return Optional.empty(); }
        @Override public List<com.devedu.learningplatform.domain.contest.ContestProblemDefinition> findAll(UUID contestId) { return List.of(); }
    };
    private final ContestRepository repository;
    private final ProgrammingProblemRepository problems;
    private final ContestSubmissionPort submissions;
    private final ContestProblemRepository contestProblems;
    private final CodeJudgeUseCase codeJudge;
    private final UserRepository users;
    private final Clock clock;
    private final com.devedu.learningplatform.application.contest.scoring.ContestScoring scoring =
            new com.devedu.learningplatform.application.contest.scoring.ContestScoring();
    public ContestService(ContestRepository repository, ProgrammingProblemRepository problems,
                          ContestSubmissionPort submissions, Clock clock) {
        this(repository, problems, submissions, null, null, null, clock);
    }
    public ContestService(ContestRepository repository, ProgrammingProblemRepository problems,
                          ContestSubmissionPort submissions, UserRepository users, Clock clock) {
        this(repository, problems, submissions, users, null, null, clock);
    }
    public ContestService(ContestRepository repository, ProgrammingProblemRepository problems,
                          ContestSubmissionPort submissions, UserRepository users,
                          ContestProblemRepository contestProblems, CodeJudgeUseCase codeJudge, Clock clock) {
        this.repository = repository; this.problems = problems; this.submissions = submissions;
        this.users = users; this.contestProblems = contestProblems == null ? NO_CONTEST_PROBLEMS : contestProblems;
        this.codeJudge = codeJudge; this.clock = clock;
    }

    @Override public ContestViews.Catalog list(Contest.Status status) {
        return list(status,null,"START_TIME");
    }
    @Override public ContestViews.Catalog list(Contest.Status status, Contest.Difficulty difficulty, String sort) {
        if (!Set.of("START_TIME","PARTICIPANTS","POPULARITY").contains(sort)) throw new IllegalArgumentException("Invalid sort");
        var now = clock.instant();
        if (status == Contest.Status.FINISHED) repository.privatizeFinished(now);
        var contests = repository.search(status, now, difficulty, sort);
        var counts = repository.participantCounts(contests.stream().map(Contest::id).toList());
        return new ContestViews.Catalog(contests.stream().map(c -> summary(c, now, counts.getOrDefault(c.id(), 0L))).toList(), now);
    }

    @Override public ContestViews.ManagedCatalog managed(AuthenticatedUser creator) {
        requireOrganizer(creator);
        var now = clock.instant();
        repository.privatizeFinished(now);
        var owned = creator.role() == UserRole.ADMIN ? repository.all() : repository.ownedBy(creator.id());
        return managedCatalog(owned, now, true);
    }

    @Override public ContestViews.ManagedCatalog supervised(AuthenticatedUser supervisor) {
        requireOrganizer(supervisor);
        var now = clock.instant();
        if (supervisor.role() == UserRole.ADMIN) return new ContestViews.ManagedCatalog(List.of(), now);
        return managedCatalog(repository.supervisedBy(supervisor.id()), now, false);
    }

    private ContestViews.ManagedCatalog managedCatalog(List<Contest> contests, Instant now, boolean editable) {
        var counts = repository.participantCounts(contests.stream().map(Contest::id).toList());
        var rows = contests.stream().map(contest -> new ContestViews.ManagedSummary(contest.id(), contest.name(), contest.type(),
                contest.startsAt(), contest.endsAt(), contest.durationMinutes(), contest.problems().size(),
                counts.getOrDefault(contest.id(), 0L), contest.statusAt(now), contest.difficulty(), contest.description(),
                contest.monitoringEnabled(), contest.published(),
                contest.statusAt(now) == Contest.Status.FINISHED || !contest.published()
                        ? Contest.Visibility.PRIVATE : contest.visibility(),
                contest.participationMode(), editable,
                contest.createdAt())).toList();
        return new ContestViews.ManagedCatalog(rows, now);
    }

    @Override public UUID create(Create command, AuthenticatedUser creator) {
        if (creator == null || (creator.role() != UserRole.TEACHER && creator.role() != UserRole.ADMIN)) {
            throw new ContestException(FORBIDDEN, "Only teachers and admins can create contests");
        }
        if (command == null) throw new IllegalArgumentException("Contest is required");
        var requestedProblems = command.problems() == null ? List.<Contest.Problem>of() : command.problems();
        requireTenPointTotal(requestedProblems);
        var contest = new Contest(UUID.randomUUID(), command.name(), command.type(), command.startsAt(),
                command.durationMinutes(), creator.id(), requestedProblems, command.difficulty(),
                command.endsAt() != null ? command.endsAt() : command.startsAt() == null ? null : command.startsAt().plusSeconds(command.durationMinutes() * 60L),
                command.description(), command.monitoringEnabled(), false, Contest.Visibility.PRIVATE,
                Contest.ParticipationMode.OPEN, clock.instant());
        requireAvailableProblems(contest.id(), requestedProblems);
        return repository.save(contest).id();
    }

    @Override public ContestViews.Manage manage(UUID id, AuthenticatedUser editor) {
        var contest = editableContest(id, editor);
        var proctorIds = repository.proctorIds(id);
        var proctors = requireUsers().findAllByIds(proctorIds).stream().map(this::userOption).toList();
        return new ContestViews.Manage(contest.id(), contest.name(), contest.type(), contest.startsAt(), contest.endsAt(),
                contest.durationMinutes(), contest.difficulty(), contest.description(), contest.monitoringEnabled(),
                contest.published(), contest.visibility(), contest.participationMode(), contest.problems(), proctors,
                repository.allowedEmails(id), true, clock.instant(), localProblemViews(id));
    }

    @Override public List<ContestViews.UserOption> searchUsers(UserRole role, String query, AuthenticatedUser editor) {
        requireOrganizer(editor);
        if (role != UserRole.TEACHER && role != UserRole.STUDENT) throw new IllegalArgumentException("Role must be TEACHER or STUDENT");
        return requireUsers().searchByRole(role, query, 30).stream().map(this::userOption).toList();
    }

    @Override public void updateDetails(UUID id, UpdateDetails command, AuthenticatedUser editor) {
        var current = editableContest(id, editor);
        if (command == null) throw new IllegalArgumentException("Contest details are required");
        repository.save(new Contest(current.id(), command.name(), command.type(), command.startsAt(), command.durationMinutes(),
                current.createdBy(), current.problems(), command.difficulty(), command.endsAt(), command.description(),
                command.monitoringEnabled(), current.published(), current.visibility(), current.participationMode(), current.createdAt()));
    }

    @Override public void updateProblems(UUID id, List<Contest.Problem> problemRefs, AuthenticatedUser editor) {
        var current = editableContest(id, editor);
        var refs = problemRefs == null ? List.<Contest.Problem>of() : List.copyOf(problemRefs);
        requireTenPointTotal(refs);
        requireAvailableProblems(id, refs);
        repository.save(copy(current, refs, current.published(), current.visibility(), current.participationMode()));
    }

    @Override public ProgrammingProblem createLocalProblem(UUID id, CreateProgrammingProblemCommand command, AuthenticatedUser editor) {
        editableContest(id, editor);
        Objects.requireNonNull(command, "Contest problem is required");
        var slug = command.slug() == null ? "" : command.slug().trim().toLowerCase(Locale.ROOT);
        if (!slug.matches("[a-z0-9]+(?:-[a-z0-9]+)*") || slug.length() > 120) {
            throw new IllegalArgumentException("Slug must contain at most 120 lowercase letters, numbers and hyphens");
        }
        if (requireContestProblems().findAll(id).stream().anyMatch(value -> value.problem().slug().equals(slug))) {
            throw new IllegalArgumentException("Slug already exists in this contest");
        }
        if (command.testCases() == null || command.testCases().size() < 3 || command.testCases().size() > 50) {
            throw new IllegalArgumentException("Contest problem must have between 3 and 50 test cases");
        }
        requireLength(command.title(), "Title", 180);
        requireLength(command.summary(), "Summary", 500);
        requireLength(command.description(), "Description", 50_000);
        requireLength(command.inputDescription(), "Input description", 50_000);
        requireLength(command.outputDescription(), "Output description", 50_000);
        requireMaximumLength(command.sampleInput(), "Sample input", 100_000);
        requireMaximumLength(command.sampleOutput(), "Sample output", 100_000);
        if (command.allowedLanguages() == null || command.allowedLanguages().isEmpty() || command.starterCodes() == null) {
            throw new IllegalArgumentException("At least one allowed language and starter code are required");
        }
        for (var language : command.allowedLanguages()) {
            requireLength(command.starterCodes().get(language), "Starter code for " + language, 100_000);
        }
        var problemId = UUID.randomUUID();
        var problem = new ProgrammingProblem(problemId, slug, command.title(), command.summary(), command.description(),
                command.inputDescription(), command.outputDescription(), command.sampleInput(), command.sampleOutput(),
                command.topic(), command.difficulty(), command.allowedLanguages(), command.starterCodes(), clock.instant());
        var testCases = java.util.stream.IntStream.range(0, command.testCases().size()).mapToObj(index -> {
            var value = Objects.requireNonNull(command.testCases().get(index), "Test case is required");
            requireMaximumLength(value.input(), "Test case input", 100_000);
            requireMaximumLength(value.expectedOutput(), "Expected output", 100_000);
            return new ProblemTestCase(UUID.randomUUID(), problemId, value.input(), value.expectedOutput(), value.timeLimitMillis(), index + 1);
        }).toList();
        return requireContestProblems().save(new com.devedu.learningplatform.domain.contest.ContestProblemDefinition(id, problem, testCases)).problem();
    }

    @Override public ProgrammingProblem problem(UUID id, UUID problemId, AuthenticatedUser viewer) {
        var contest = find(id);
        if (!contest.published() && !canManage(contest, viewer)) throw new ContestException(NOT_FOUND, "Contest not found");
        if (contest.problems().stream().noneMatch(value -> value.problemId().equals(problemId))) {
            throw new ContestException(NOT_FOUND, "Problem is not part of this contest");
        }
        return findProblem(id, problemId).orElseThrow(() -> new ContestException(NOT_FOUND, "Problem is unavailable"));
    }

    @Override public JudgeResult run(UUID id, UUID problemId, CodeLanguage language, String code, AuthenticatedUser user) {
        requireUser(user);
        var contest = requireActiveAttempt(id, user);
        if (contest.problems().stream().noneMatch(value -> value.problemId().equals(problemId))) throw new ContestException(NOT_FOUND, "Problem is not part of this contest");
        var local = requireContestProblems().find(id, problemId).orElseThrow(() -> new ContestException(NOT_FOUND, "Contest problem is unavailable"));
        requireCode(local.problem(), language, code);
        return requireJudge().judge(new JudgeSubmissionCommand(UUID.randomUUID(), language, code, local.testCases()));
    }

    @Override public void delete(UUID id, AuthenticatedUser editor) {
        editableContest(id, editor);
        repository.softDelete(id, clock.instant());
    }

    @Override public void updateSettings(UUID id, UpdateSettings command, AuthenticatedUser editor) {
        var current = editableContest(id, editor);
        if (command == null || command.visibility() == null || command.participationMode() == null) {
            throw new IllegalArgumentException("Contest settings are required");
        }
        var emails = (command.allowedEmails() == null ? List.<String>of() : command.allowedEmails()).stream()
                .map(User::normalizeEmail).distinct().limit(1000).toList();
        var publicIds = command.proctorPublicIds() == null ? List.<Long>of() : command.proctorPublicIds().stream().distinct().toList();
        if (publicIds.size() > 50) throw new IllegalArgumentException("A contest can have at most 50 supervising teachers");
        var proctors = publicIds.stream().map(publicId -> requireUsers().findByPublicId(publicId)
                        .filter(user -> user.role() == UserRole.TEACHER)
                        .orElseThrow(() -> new IllegalArgumentException("Supervising teacher is unavailable")))
                .map(User::id).filter(userId -> !userId.equals(current.createdBy())).toList();
        if (command.visibility() == Contest.Visibility.PUBLIC) {
            requirePublishable(current, command.participationMode(), emails);
        }
        var published = current.published() || command.visibility() == Contest.Visibility.PUBLIC;
        repository.saveConfiguration(copy(current, current.problems(), published, command.visibility(), command.participationMode()), proctors, emails);
    }

    @Override public void publish(UUID id, AuthenticatedUser editor) {
        var current = editableContest(id, editor);
        if (current.published()) return;
        requirePublishable(current, current.participationMode(), repository.allowedEmails(id));
        repository.save(copy(current, current.problems(), true, current.visibility(), current.participationMode()));
    }

    private void requirePublishable(Contest contest, Contest.ParticipationMode mode, List<String> allowedEmails) {
        if (contest.problems().isEmpty()) throw new IllegalArgumentException("Add at least one problem before publishing");
        requireTenPointTotal(contest.problems());
        if (!clock.instant().isBefore(contest.endsAt())) throw new IllegalArgumentException("Contest end time must be in the future when publishing");
        if (mode == Contest.ParticipationMode.RESTRICTED && allowedEmails.isEmpty()) {
            throw new IllegalArgumentException("Add at least one allowed email for a restricted contest");
        }
    }

    private void requireTenPointTotal(List<Contest.Problem> refs) {
        if (!refs.isEmpty() && (refs.size() > 10 || refs.stream().mapToInt(Contest.Problem::points).sum() != 10)) {
            throw new IllegalArgumentException("Contest problems must have exactly 10 total points");
        }
    }

    @Override public void register(UUID id, AuthenticatedUser user) {
        requireUser(user);
        var contest = find(id);
        requirePublished(contest);
        var now = clock.instant();
        if (contest.statusAt(now) == Contest.Status.FINISHED) throw new ContestException(CONFLICT, "Contest has ended");
        if (contest.participationMode() == Contest.ParticipationMode.RESTRICTED && !repository.emailAllowed(id, user.email())) {
            throw new ContestException(FORBIDDEN, "Email của bạn không nằm trong danh sách thí sinh được phép tham gia");
        }
        repository.register(id, user.id(), now);
    }

    @Override public ProblemSubmission submit(UUID id, UUID problemId, CodeLanguage language, String code, AuthenticatedUser user) {
        return submit(id, problemId, language, code, user, null);
    }
    @Override public ContestAttempt startAttempt(UUID id, AuthenticatedUser user) {
        requireUser(user);
        var contest = find(id);
        requirePublished(contest);
        var now = clock.instant();
        if (contest.statusAt(now) != Contest.Status.ONGOING) throw new ContestException(CONFLICT, "Cuộc thi chưa bắt đầu hoặc đã kết thúc.");
        if (!repository.registered(id, user.id())) throw new ContestException(FORBIDDEN, "Đăng ký cuộc thi trước khi bắt đầu làm bài.");
        return ContestAttempt.startingAt(contest, repository.startAttempt(id, user.id(), now));
    }
    @Override public ProblemSubmission submit(UUID id, UUID problemId, CodeLanguage language, String code, AuthenticatedUser user, String requestKey) {
        requireUser(user);
        var receivedAt = clock.instant();
        var contest = requireActiveAttempt(id, user);
        if (contest.problems().stream().noneMatch(p -> p.problemId().equals(problemId))) throw new ContestException(NOT_FOUND, "Problem is not part of this contest");
        var local = requireContestProblems().find(id, problemId);
        if (local.isPresent()) {
            requireCode(local.get().problem(), language, code);
            return submissions.submitLocal(id, local.get(), user.id(), language, code, receivedAt, null, requestKey);
        }
        var problem = problems.findById(problemId).orElseThrow(() -> new ContestException(NOT_FOUND, "Problem is unavailable"));
        if (language == null || code == null || code.isBlank() || code.length() > 100000) {
            throw new IllegalArgumentException("Language and source code (up to 100000 characters) are required");
        }
        var command = new SubmitProblemCommand(user.id(), problem.slug(), language, code);
        return requestKey == null ? submissions.submit(id, command, receivedAt) : submissions.submit(id, command, receivedAt, null, requestKey);
    }

    @Override public ContestViews.Detail detail(UUID id, AuthenticatedUser viewer) {
        var contest = find(id);
        if (!contest.published() && !canManage(contest, viewer)) throw new ContestException(NOT_FOUND, "Contest not found");
        var now = clock.instant();
        var people = repository.participants(id);
        var entries = repository.entries(id);
        var catalog = problems.findAllByIds(contest.problems().stream().map(Contest.Problem::problemId).toList())
                .stream().collect(Collectors.toMap(ProgrammingProblem::id, p -> p));
        var localIds = new HashSet<UUID>();
        requireContestProblems().findAll(id).forEach(value -> {
            catalog.put(value.problem().id(), value.problem());
            localIds.add(value.problem().id());
        });
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
                    String.valueOf((char) ('A' + i)), ref.points(), solved, counts.getOrDefault(ref.problemId(), 0L), problem != null,
                    localIds.contains(ref.problemId())));
        }
        var rowMap = rows.stream().collect(Collectors.toMap(ContestViews.Problem::id, p -> p));
        var mine = entries.stream().filter(e -> viewer != null && e.userId().equals(viewer.id()))
                .sorted(Comparator.comparing(ContestRepository.Entry::receivedAt).reversed())
                .map(e -> new ContestViews.Submission(e.id(), e.problemId(), rowMap.get(e.problemId()).letter(),
                        rowMap.get(e.problemId()).title(), e.language(), e.status(), e.passedTests(), e.totalTests(),
                        e.executionTimeMillis(), e.receivedAt(), e.status() == SubmissionStatus.ACCEPTED ? points.get(e.problemId()) : 0,
                        points.get(e.problemId()))).toList();
        var attempt = myParticipant.map(ContestRepository.Participant::startedAt).map(at -> ContestAttempt.startingAt(contest, at)).orElse(null);
        return new ContestViews.Detail(summary(contest, now, people.size()), now, myParticipant.isPresent(), rows, board, myResult, mine, scoring.rules(), attempt);
    }

    private Contest find(UUID id) { return repository.find(id).orElseThrow(() -> new ContestException(NOT_FOUND, "Contest not found")); }
    private Contest requireActiveAttempt(UUID id, AuthenticatedUser user) {
        var contest = find(id);
        requirePublished(contest);
        var now = clock.instant();
        if (contest.statusAt(now) != Contest.Status.ONGOING) throw new ContestException(CONFLICT, "Contest is not accepting submissions");
        if (!repository.registered(id, user.id())) throw new ContestException(FORBIDDEN, "Register for this contest before submitting");
        var startedAt = repository.attemptStartedAt(id, user.id()).orElseThrow(() -> new ContestException(CONFLICT, "Start your contest attempt before running code"));
        if (!ContestAttempt.startingAt(contest, startedAt).activeAt(now)) throw new ContestException(CONFLICT, "Your contest attempt has ended");
        return contest;
    }
    private Optional<ProgrammingProblem> findProblem(UUID contestId, UUID problemId) {
        var local = requireContestProblems().find(contestId, problemId)
                .map(com.devedu.learningplatform.domain.contest.ContestProblemDefinition::problem);
        return local.isPresent() ? local : problems.findById(problemId);
    }
    private void requireCode(ProgrammingProblem problem, CodeLanguage language, String code) {
        if (language == null || !problem.allowedLanguages().contains(language)) throw new IllegalArgumentException("Language is not allowed for this problem");
        if (code == null || code.isBlank() || code.length() > 100000) throw new IllegalArgumentException("Source code (up to 100000 characters) is required");
    }
    private void requireLength(String value, String field, int maximum) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        requireMaximumLength(value, field, maximum);
    }
    private void requireMaximumLength(String value, String field, int maximum) {
        if (value != null && value.length() > maximum) throw new IllegalArgumentException(field + " must not exceed " + maximum + " characters");
    }
    private void requireAvailableProblems(UUID contestId, List<Contest.Problem> refs) {
        var ids = refs.stream().map(Contest.Problem::problemId).collect(Collectors.toSet());
        var available = problems.findAllByIds(new ArrayList<>(ids)).stream().map(ProgrammingProblem::id).collect(Collectors.toSet());
        requireContestProblems().findAll(contestId).stream().map(value -> value.problem().id()).forEach(available::add);
        if (!available.containsAll(ids)) throw new IllegalArgumentException("One or more problems are unavailable");
    }
    private List<ContestViews.LocalProblem> localProblemViews(UUID contestId) {
        return requireContestProblems().findAll(contestId).stream().map(value -> new ContestViews.LocalProblem(value.problem().id(),
                value.problem().slug(), value.problem().title(), value.problem().difficulty())).toList();
    }
    private ContestProblemRepository requireContestProblems() {
        return contestProblems;
    }
    private CodeJudgeUseCase requireJudge() {
        if (codeJudge == null) throw new IllegalStateException("Code judge is required");
        return codeJudge;
    }
    private Contest editableContest(UUID id, AuthenticatedUser editor) {
        requireOrganizer(editor);
        var contest = find(id);
        if (!canManage(contest, editor)) throw new ContestException(FORBIDDEN, "Only the contest owner or an admin can edit this contest");
        return contest;
    }
    private boolean canManage(Contest contest, AuthenticatedUser user) {
        return user != null && (user.role() == UserRole.ADMIN || contest.createdBy().equals(user.id()));
    }
    private void requireOrganizer(AuthenticatedUser user) {
        if (user == null || user.role() != UserRole.TEACHER && user.role() != UserRole.ADMIN) {
            throw new ContestException(FORBIDDEN, "Only teachers and admins can manage contests");
        }
    }
    private void requirePublished(Contest contest) {
        if (!contest.published()) throw new ContestException(CONFLICT, "Contest has not been published");
    }
    private UserRepository requireUsers() {
        if (users == null) throw new IllegalStateException("User repository is required for contest management");
        return users;
    }
    private Contest copy(Contest c, List<Contest.Problem> refs, boolean published, Contest.Visibility visibility,
                         Contest.ParticipationMode participationMode) {
        return new Contest(c.id(), c.name(), c.type(), c.startsAt(), c.durationMinutes(), c.createdBy(), refs,
                c.difficulty(), c.endsAt(), c.description(), c.monitoringEnabled(), published, visibility,
                participationMode, c.createdAt());
    }
    private ContestViews.UserOption userOption(User user) {
        var code = user.role() == UserRole.STUDENT ? user.studentCode() : user.teacherCode();
        return new ContestViews.UserOption(user.publicId(), user.name(), user.email(), user.role(), code);
    }
    private void requireUser(AuthenticatedUser user) { if (user == null) throw new ContestException(FORBIDDEN, "Sign in to participate"); }
    private ContestViews.Summary summary(Contest c, Instant now, long count) {
        return new ContestViews.Summary(c.id(), c.name(), c.type(), c.startsAt(), c.endsAt(), c.durationMinutes(),
                c.problems().size(), count, c.statusAt(now), c.difficulty(), c.description(),
                c.monitoringEnabled(), c.createdAt(), c.participationMode());
    }
}
