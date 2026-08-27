package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.port.in.ProgrammingProblemsUseCase;
import com.devedu.learningplatform.application.port.in.command.SubmitProblemCommand;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.domain.model.ProblemTopic;
import com.devedu.learningplatform.domain.model.ProblemDifficulty;
import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ProgrammingProblem;
import com.devedu.learningplatform.presentation.rest.dto.ProblemSubmissionResponse;
import com.devedu.learningplatform.presentation.rest.dto.ProgrammingProblemDetailResponse;
import com.devedu.learningplatform.presentation.rest.dto.ProgrammingProblemSummaryResponse;
import com.devedu.learningplatform.presentation.rest.dto.SubmitProblemRequest;
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

    public ProgrammingProblemsController(ProgrammingProblemsUseCase programmingProblemsUseCase) {
        this.programmingProblemsUseCase = programmingProblemsUseCase;
    }

    @GetMapping
    public List<ProgrammingProblemSummaryResponse> list(
            @RequestParam(required = false) ProblemTopic topic,
            @RequestParam(required = false) ProblemDifficulty difficulty,
            @RequestParam(required = false) CodeLanguage language
    ) {
        return programmingProblemsUseCase.list(topic, difficulty, language).stream()
                .map(this::toSummaryResponse)
                .toList();
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
                problem.sampleInput(),
                problem.sampleOutput(),
                problem.topic(),
                problem.difficulty(),
                problem.allowedLanguages(),
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

    private ProgrammingProblemSummaryResponse toSummaryResponse(ProgrammingProblem problem) {
        return new ProgrammingProblemSummaryResponse(
                problem.id(),
                problem.slug(),
                problem.title(),
                problem.summary(),
                problem.topic(),
                problem.difficulty(),
                problem.allowedLanguages()
        );
    }
}
