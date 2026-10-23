package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.port.in.GenerateProgrammingProblemDraftUseCase;
import com.devedu.learningplatform.presentation.rest.dto.GenerateProgrammingProblemDraftRequest;
import com.devedu.learningplatform.presentation.rest.dto.GeneratedProgrammingProblemDraftResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher/problems/ai-draft")
public class TeacherProgrammingProblemAiController {

    private final GenerateProgrammingProblemDraftUseCase useCase;

    public TeacherProgrammingProblemAiController(GenerateProgrammingProblemDraftUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public ResponseEntity<GeneratedProgrammingProblemDraftResponse> generate(
            @RequestBody GenerateProgrammingProblemDraftRequest request
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(GeneratedProgrammingProblemDraftResponse.from(useCase.generate(request.description())));
    }
}
