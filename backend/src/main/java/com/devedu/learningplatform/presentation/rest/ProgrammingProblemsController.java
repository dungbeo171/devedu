package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.port.in.ProgrammingProblemsUseCase;
import com.devedu.learningplatform.application.port.in.command.SubmitProblemCommand;
import com.devedu.learningplatform.application.port.in.command.RunProblemTestsCommand;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.domain.model.ProblemTopic;
import com.devedu.learningplatform.domain.model.ProblemDifficulty;
import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.application.port.in.result.ProgrammingProblemListItem;
import com.devedu.learningplatform.presentation.rest.dto.ProblemSubmissionResponse;
import com.devedu.learningplatform.presentation.rest.dto.ProgrammingProblemDetailResponse;
import com.devedu.learningplatform.presentation.rest.dto.ProgrammingProblemSummaryResponse;
import com.devedu.learningplatform.presentation.rest.dto.SubmitProblemRequest;
import com.devedu.learningplatform.presentation.rest.dto.ProblemTestCaseResultResponse;
import com.devedu.learningplatform.presentation.rest.dto.ProblemTestRunResponse;
import com.devedu.learningplatform.application.problemrecommendation.ProblemRecommendationUseCase;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/problems")
public class ProgrammingProblemsController {

    private final ProgrammingProblemsUseCase programmingProblemsUseCase;
    private final ProblemRecommendationUseCase problemRecommendationUseCase;

    public ProgrammingProblemsController(
            ProgrammingProblemsUseCase programmingProblemsUseCase,
            ProblemRecommendationUseCase problemRecommendationUseCase
    ) {
        this.programmingProblemsUseCase = programmingProblemsUseCase;
        this.problemRecommendationUseCase = problemRecommendationUseCase;
    }

    @GetMapping
    public ResponseEntity<List<ProgrammingProblemSummaryResponse>> list(
            @RequestParam(required = false) ProblemTopic topic,
            @RequestParam(required = false) ProblemDifficulty difficulty,
            @RequestParam(required = false) CodeLanguage language,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        var result = user == null
                ? programmingProblemsUseCase.list(topic, difficulty, language).stream()
                        .map(this::toSummaryResponse)
                        .toList()
                : problemRecommendationUseCase.recommend(user.id(), topic, difficulty, language).stream()
                .map(item -> {
                    var problem = item.problem();
                    return new ProgrammingProblemSummaryResponse(
                            problem.id(), problem.slug(), problem.title(), problem.summary(),
                            problem.topic(), problem.difficulty(), problem.allowedLanguages(),
                            item.acceptanceRate()
                    );
                })
                .toList();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(result);
    }

    @GetMapping("/{slug}")
    public ProgrammingProblemDetailResponse getBySlug(@PathVariable String slug) {
        var problem = programmingProblemsUseCase.getBySlug(slug);
        return new ProgrammingProblemDetailResponse(
                problem.id(),
                problem.slug(),
                problem.title(),
                problem.summary(),
                problem.description(),
                problem.inputDescription(),
                problem.outputDescription(),
                problem.sampleInput(),
                problem.sampleOutput(),
                problem.topic(),
                problem.difficulty(),
                problem.allowedLanguages(),
                problem.starterCodes(),
                problem.createdAt()
        );
    }

    @PostMapping("/{slug}/submissions")
    public ResponseEntity<ProblemSubmissionResponse> submit(
            @PathVariable String slug,
            @RequestBody SubmitProblemRequest request,
            @AuthenticationPrincipal AuthenticatedUser student
    ) {
        var submission = programmingProblemsUseCase.submit(
                new SubmitProblemCommand(student.id(), slug, request.language(), request.sourceCode())
        );
        return ResponseEntity.ok(new ProblemSubmissionResponse(
                submission.id(),
                submission.problemId(),
                submission.language(),
                submission.status(),
                submission.diagnostic(),
                submission.passedTests(),
                submission.totalTests(),
                submission.executionTimeMillis(),
                submission.submittedAt()
        ));
    }

    @PostMapping("/{slug}/runs")
    public ProblemTestRunResponse runTests(
            @PathVariable String slug,
            @RequestBody SubmitProblemRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        var result = programmingProblemsUseCase.runTests(
                new RunProblemTestsCommand(slug, request.language(), request.sourceCode(), user.id())
        );
        return new ProblemTestRunResponse(
                result.status(),
                result.diagnostic(),
                result.passedTests(),
                result.totalTests(),
                result.executionTimeMillis(),
                result.testCases().stream()
                        .map(testCase -> new ProblemTestCaseResultResponse(
                                testCase.position(), testCase.passed(), testCase.status(),
                                testCase.position() == 1 ? testCase.actualOutput() : null))
                        .toList()
        );
    }

    private ProgrammingProblemSummaryResponse toSummaryResponse(ProgrammingProblemListItem item) {
        var problem = item.problem();
        return new ProgrammingProblemSummaryResponse(
                problem.id(),
                problem.slug(),
                problem.title(),
                problem.summary(),
                problem.topic(),
                problem.difficulty(),
                problem.allowedLanguages(),
                item.acceptanceRate()
        );
    }
}
