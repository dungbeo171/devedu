package com.devedu.learningplatform.presentation.rest.contest;

import com.devedu.learningplatform.application.contest.*;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.presentation.rest.dto.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
public class ContestExperienceController {
    private final ContestExperienceUseCase contests;
    public ContestExperienceController(ContestExperienceUseCase contests) { this.contests=contests; }
    private <T> ResponseEntity<T> response(T body) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body); }
    @GetMapping("/api/contests/{id}/results")
    public ResponseEntity<ContestExperienceViews.Result> results(@PathVariable UUID id,@AuthenticationPrincipal AuthenticatedUser user) { return response(contests.results(id,user)); }
    @GetMapping("/api/profiles/{handle}/contests")
    public ResponseEntity<ContestExperienceViews.Profile> profile(@PathVariable String handle,@AuthenticationPrincipal AuthenticatedUser user) {
        if(!handle.matches("user--?[0-9]{1,19}")) throw new IllegalArgumentException("Invalid profile handle");
        try { return response(contests.profile(Long.parseLong(handle.substring(5)),user)); }
        catch(NumberFormatException e) { throw new IllegalArgumentException("Invalid profile handle"); }
    }
    @PostMapping("/api/contests/{id}/virtual")
    public ResponseEntity<ContestExperienceViews.VirtualSession> start(@PathVariable UUID id,@AuthenticationPrincipal AuthenticatedUser user) { return response(contests.startVirtual(id,user)); }
    @GetMapping("/api/contests/{id}/virtual")
    public ResponseEntity<ContestExperienceViews.VirtualSession> virtual(@PathVariable UUID id,@RequestParam(required=false) UUID sessionId,@AuthenticationPrincipal AuthenticatedUser user) { return response(contests.virtualDetail(id,sessionId,user)); }
    @PostMapping("/api/contests/{id}/virtual/{sessionId}/problems/{problemId}/submissions")
    public ResponseEntity<ProblemSubmissionResponse> submit(@PathVariable UUID id,@PathVariable UUID sessionId,@PathVariable UUID problemId,
            @RequestHeader(value="Idempotency-Key",required=false) String requestKey,@RequestBody SubmitProblemRequest body,@AuthenticationPrincipal AuthenticatedUser user) {
        var s=contests.submitVirtual(id,sessionId,problemId,body.language(),body.sourceCode(),requestKey,user);
        return response(new ProblemSubmissionResponse(s.id(),s.problemId(),s.language(),s.status(),s.diagnostic(),s.passedTests(),s.totalTests(),s.executionTimeMillis(),s.submittedAt()));
    }
}
