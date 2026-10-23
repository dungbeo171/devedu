package com.devedu.learningplatform.presentation.rest.contest;

import com.devedu.learningplatform.application.contest.*;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.domain.contest.Contest;
import com.devedu.learningplatform.domain.model.UserRole;
import com.devedu.learningplatform.presentation.rest.dto.SubmitProblemRequest;
import com.devedu.learningplatform.presentation.rest.dto.ProblemSubmissionResponse;
import com.devedu.learningplatform.presentation.rest.dto.CreateProgrammingProblemRequest;
import com.devedu.learningplatform.presentation.rest.dto.ProgrammingProblemDetailResponse;
import com.devedu.learningplatform.presentation.rest.dto.ProblemTestRunResponse;
import com.devedu.learningplatform.presentation.rest.dto.ProblemTestCaseResultResponse;
import com.devedu.learningplatform.application.port.in.command.CreateProgrammingProblemCommand;
import com.devedu.learningplatform.application.port.in.command.CreateProblemTestCaseCommand;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestController
public class ContestController {
    private final ContestUseCase contests;
    public ContestController(ContestUseCase contests) { this.contests = contests; }

    @GetMapping("/api/contests")
    public ContestViews.Catalog list(@RequestParam(defaultValue = "UPCOMING") Contest.Status status,
            @RequestParam(required=false) Contest.Difficulty difficulty,
            @RequestParam(defaultValue="START_TIME") String sort) { return contests.list(status,difficulty,sort); }

    @GetMapping("/api/contests/{id}")
    public ResponseEntity<ContestViews.Detail> detail(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser viewer) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(contests.detail(id, viewer));
    }

    @PostMapping("/api/teacher/contests")
    public ResponseEntity<Map<String, UUID>> create(@RequestBody CreateRequest body, @AuthenticationPrincipal AuthenticatedUser user) {
        if (body.problems() != null && body.problems().stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("Problems are invalid");
        var id = contests.create(new ContestUseCase.Create(body.name(), body.type(), body.startsAt(), body.durationMinutes(),
                body.problems() == null ? List.of() : body.problems().stream().map(p -> new Contest.Problem(p.problemId(), p.points())).toList(), body.difficulty(),
                body.endsAt(), body.description(), body.monitoringEnabled()), user);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    @GetMapping("/api/teacher/contests/{id}/manage")
    public ResponseEntity<ContestViews.Manage> manage(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(contests.manage(id, user));
    }

    @GetMapping("/api/teacher/contests/mine")
    public ResponseEntity<ContestViews.ManagedCatalog> managed(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(contests.managed(user));
    }

    @GetMapping("/api/teacher/contests/supervised")
    public ResponseEntity<ContestViews.ManagedCatalog> supervised(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(contests.supervised(user));
    }

    @GetMapping("/api/teacher/contest-users")
    public ResponseEntity<List<ContestViews.UserOption>> users(@RequestParam UserRole role,
            @RequestParam(defaultValue = "") String query, @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(contests.searchUsers(role, query, user));
    }

    @PutMapping("/api/teacher/contests/{id}/details")
    public ResponseEntity<Void> updateDetails(@PathVariable UUID id, @RequestBody DetailsRequest body,
            @AuthenticationPrincipal AuthenticatedUser user) {
        contests.updateDetails(id, new ContestUseCase.UpdateDetails(body.name(), body.type(), body.startsAt(), body.endsAt(),
                body.durationMinutes(), body.difficulty(), body.description(), body.monitoringEnabled()), user);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/teacher/contests/{id}/problems")
    public ResponseEntity<Void> updateProblems(@PathVariable UUID id, @RequestBody ProblemsRequest body,
            @AuthenticationPrincipal AuthenticatedUser user) {
        if (body.problems() == null || body.problems().stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("Problems are required");
        contests.updateProblems(id, body.problems().stream().map(p -> new Contest.Problem(p.problemId(), p.points())).toList(), user);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/teacher/contests/{id}/settings")
    public ResponseEntity<Void> updateSettings(@PathVariable UUID id, @RequestBody SettingsRequest body,
            @AuthenticationPrincipal AuthenticatedUser user) {
        contests.updateSettings(id, new ContestUseCase.UpdateSettings(body.visibility(), body.participationMode(),
                body.allowedEmails(), body.proctorPublicIds()), user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/teacher/contests/{id}/publish")
    public ResponseEntity<Void> publish(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        contests.publish(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/teacher/contests/{id}/local-problems")
    public ResponseEntity<ProgrammingProblemDetailResponse> createLocalProblem(@PathVariable UUID id,
            @RequestBody CreateProgrammingProblemRequest body, @AuthenticationPrincipal AuthenticatedUser user) {
        var problem = contests.createLocalProblem(id, new CreateProgrammingProblemCommand(body.slug(), body.title(),
                body.summary(), body.description(), body.inputDescription(), body.outputDescription(), body.sampleInput(),
                body.sampleOutput(), body.topic(), body.difficulty(), body.allowedLanguages(), body.starterCodes(),
                body.testCases() == null ? null : body.testCases().stream().map(test -> new CreateProblemTestCaseCommand(
                        test.input(), test.expectedOutput(), test.timeLimitMillis())).toList()), user);
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(problemResponse(problem));
    }

    @DeleteMapping("/api/teacher/contests/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        contests.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/contests/{id}/registration")
    public ResponseEntity<Void> register(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        contests.register(id, user); return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/contests/{id}/problems/{problemId}/submissions")
    public ProblemSubmissionResponse submit(@PathVariable UUID id, @PathVariable UUID problemId,
            @RequestBody SubmitProblemRequest body, @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "Idempotency-Key", required = false) String requestKey) {
        var s = contests.submit(id, problemId, body.language(), body.sourceCode(), user, requestKey);
        return new ProblemSubmissionResponse(s.id(), s.problemId(), s.language(), s.status(), s.diagnostic(),
                s.passedTests(), s.totalTests(), s.executionTimeMillis(), s.submittedAt());
    }

    @GetMapping("/api/contests/{id}/problems/{problemId}")
    public ResponseEntity<ProgrammingProblemDetailResponse> problem(@PathVariable UUID id, @PathVariable UUID problemId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(problemResponse(contests.problem(id, problemId, user)));
    }

    @PostMapping("/api/contests/{id}/problems/{problemId}/runs")
    public ProblemTestRunResponse run(@PathVariable UUID id, @PathVariable UUID problemId,
            @RequestBody SubmitProblemRequest body, @AuthenticationPrincipal AuthenticatedUser user) {
        var result = contests.run(id, problemId, body.language(), body.sourceCode(), user);
        return new ProblemTestRunResponse(result.status(), result.diagnostic(), result.passedTests(), result.totalTests(),
                result.executionTimeMillis(), result.testCases().stream().map(test -> new ProblemTestCaseResultResponse(
                        test.position(), test.passed(), test.status(), test.position() == 1 ? test.actualOutput() : null)).toList());
    }
    @PostMapping("/api/contests/{id}/attempt")
    public ResponseEntity<com.devedu.learningplatform.domain.contest.ContestAttempt> startAttempt(
            @PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(contests.startAttempt(id, user));
    }
    public record ProblemRequest(UUID problemId, int points) {}
    public record CreateRequest(String name, Contest.Type type, Instant startsAt, int durationMinutes,
                                List<ProblemRequest> problems, Contest.Difficulty difficulty,
                                Instant endsAt, String description, boolean monitoringEnabled) {}
    public record DetailsRequest(String name, Contest.Type type, Instant startsAt, Instant endsAt,
                                 int durationMinutes, Contest.Difficulty difficulty, String description,
                                 boolean monitoringEnabled) {}
    public record ProblemsRequest(List<ProblemRequest> problems) {}
    public record SettingsRequest(Contest.Visibility visibility, Contest.ParticipationMode participationMode,
                                  List<String> allowedEmails, List<Long> proctorPublicIds) {}

    private ProgrammingProblemDetailResponse problemResponse(com.devedu.learningplatform.domain.model.ProgrammingProblem problem) {
        return new ProgrammingProblemDetailResponse(problem.id(), problem.slug(), problem.title(), problem.summary(),
                problem.description(), problem.inputDescription(), problem.outputDescription(), problem.sampleInput(),
                problem.sampleOutput(), problem.topic(), problem.difficulty(), problem.allowedLanguages(),
                problem.starterCodes(), problem.createdAt());
    }
}
