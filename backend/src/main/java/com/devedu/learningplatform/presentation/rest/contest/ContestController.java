package com.devedu.learningplatform.presentation.rest.contest;

import com.devedu.learningplatform.application.contest.*;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.domain.contest.Contest;
import com.devedu.learningplatform.presentation.rest.dto.SubmitProblemRequest;
import com.devedu.learningplatform.presentation.rest.dto.ProblemSubmissionResponse;
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
            @RequestParam(required=false) Boolean rated,@RequestParam(required=false) Contest.Difficulty difficulty,
            @RequestParam(defaultValue="START_TIME") String sort) { return contests.list(status,rated,difficulty,sort); }

    @GetMapping("/api/contests/{id}")
    public ResponseEntity<ContestViews.Detail> detail(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser viewer) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(contests.detail(id, viewer));
    }

    @PostMapping("/api/teacher/contests")
    public ResponseEntity<Map<String, UUID>> create(@RequestBody CreateRequest body, @AuthenticationPrincipal AuthenticatedUser user) {
        if (body.problems() == null || body.problems().stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("Problems are required");
        var id = contests.create(new ContestUseCase.Create(body.name(), body.type(), body.startsAt(), body.durationMinutes(), body.rules(),
                body.problems().stream().map(p -> new Contest.Problem(p.problemId(), p.points())).toList(), body.rated(), body.difficulty()), user);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
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
    public record ProblemRequest(UUID problemId, int points) {}
    public record CreateRequest(String name, Contest.Type type, Instant startsAt, int durationMinutes,
                                String rules, List<ProblemRequest> problems, boolean rated, Contest.Difficulty difficulty) {}
}
