package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.port.in.ManageProgrammingProblemsUseCase;
import com.devedu.learningplatform.application.port.in.command.CreateProblemTestCaseCommand;
import com.devedu.learningplatform.application.port.in.command.UpdateProgrammingProblemCommand;
import com.devedu.learningplatform.presentation.rest.dto.CreateProblemTestCaseRequest;
import com.devedu.learningplatform.presentation.rest.dto.CreateProgrammingProblemRequest;
import com.devedu.learningplatform.presentation.rest.dto.ManagedProgrammingProblemResponse;
import com.devedu.learningplatform.presentation.rest.dto.ProgrammingProblemDetailResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/problems")
public class AdminProgrammingProblemsController {

    private final ManageProgrammingProblemsUseCase useCase;

    public AdminProgrammingProblemsController(ManageProgrammingProblemsUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/{slug}")
    public ManagedProgrammingProblemResponse getForManagement(@PathVariable String slug) {
        var managed = useCase.getForManagement(slug);
        var problem = managed.problem();
        return new ManagedProgrammingProblemResponse(
                problem.id(), problem.slug(), problem.title(), problem.summary(), problem.description(),
                problem.sampleInput(), problem.sampleOutput(), problem.topic(), problem.difficulty(),
                problem.allowedLanguages(), problem.starterCodes(), problem.createdAt(),
                managed.testCases().stream()
                        .map(testCase -> new CreateProblemTestCaseRequest(
                                testCase.input(), testCase.expectedOutput(), testCase.timeLimitMillis()))
                        .toList()
        );
    }

    @PutMapping("/{slug}")
    public ProgrammingProblemDetailResponse update(
            @PathVariable String slug,
            @RequestBody CreateProgrammingProblemRequest request
    ) {
        var problem = useCase.update(new UpdateProgrammingProblemCommand(
                slug, request.slug(), request.title(), request.summary(), request.description(),
                request.sampleInput(), request.sampleOutput(), request.topic(), request.difficulty(),
                request.allowedLanguages(), request.starterCodes(), toTestCases(request)
        ));
        return new ProgrammingProblemDetailResponse(
                problem.id(), problem.slug(), problem.title(), problem.summary(), problem.description(),
                problem.sampleInput(), problem.sampleOutput(), problem.topic(), problem.difficulty(),
                problem.allowedLanguages(), problem.starterCodes(), problem.createdAt()
        );
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> delete(@PathVariable String slug) {
        useCase.delete(slug);
        return ResponseEntity.noContent().build();
    }

    private java.util.List<CreateProblemTestCaseCommand> toTestCases(CreateProgrammingProblemRequest request) {
        return request.testCases() == null ? null : request.testCases().stream()
                .map(testCase -> new CreateProblemTestCaseCommand(
                        testCase.input(), testCase.expectedOutput(), testCase.timeLimitMillis()))
                .toList();
    }
}
