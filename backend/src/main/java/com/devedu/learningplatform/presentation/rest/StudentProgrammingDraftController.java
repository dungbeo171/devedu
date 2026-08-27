package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.port.in.GetProblemDraftUseCase;
import com.devedu.learningplatform.application.port.in.SaveProblemDraftUseCase;
import com.devedu.learningplatform.application.port.in.command.SaveProblemDraftCommand;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.presentation.rest.dto.ProblemDraftResponse;
import com.devedu.learningplatform.presentation.rest.dto.SaveProblemDraftRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/problems")
public class StudentProgrammingDraftController {

    private final GetProblemDraftUseCase getDraftUseCase;
    private final SaveProblemDraftUseCase saveDraftUseCase;

    public StudentProgrammingDraftController(
            GetProblemDraftUseCase getDraftUseCase,
            SaveProblemDraftUseCase saveDraftUseCase
    ) {
        this.getDraftUseCase = getDraftUseCase;
        this.saveDraftUseCase = saveDraftUseCase;
    }

    @GetMapping("/{slug}/draft")
    public ResponseEntity<ProblemDraftResponse> getDraft(
            @PathVariable String slug,
            @AuthenticationPrincipal AuthenticatedUser student
    ) {
        return getDraftUseCase.getDraft(student.id(), slug)
                .map(ProblemDraftResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/{slug}/draft")
    public ProblemDraftResponse saveDraft(
            @PathVariable String slug,
            @RequestBody SaveProblemDraftRequest request,
            @AuthenticationPrincipal AuthenticatedUser student
    ) {
        return ProblemDraftResponse.from(saveDraftUseCase.saveDraft(new SaveProblemDraftCommand(
                student.id(),
                slug,
                request.language(),
                request.sourceCode(),
                request.input()
        )));
    }
}
