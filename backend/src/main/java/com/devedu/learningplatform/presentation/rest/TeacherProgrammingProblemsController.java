package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.port.in.CreateProgrammingProblemUseCase;
import com.devedu.learningplatform.application.port.in.command.CreateProblemTestCaseCommand;
import com.devedu.learningplatform.application.port.in.command.CreateProgrammingProblemCommand;
import com.devedu.learningplatform.presentation.rest.dto.CreateProgrammingProblemRequest;
import com.devedu.learningplatform.presentation.rest.dto.ProgrammingProblemDetailResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher/problems")
public class TeacherProgrammingProblemsController {

    private final CreateProgrammingProblemUseCase createProgrammingProblemUseCase;

    public TeacherProgrammingProblemsController(CreateProgrammingProblemUseCase createProgrammingProblemUseCase) {
        this.createProgrammingProblemUseCase = createProgrammingProblemUseCase;
    }

    @PostMapping
    public ResponseEntity<ProgrammingProblemDetailResponse> create(@RequestBody CreateProgrammingProblemRequest request) {
        var problem = createProgrammingProblemUseCase.create(new CreateProgrammingProblemCommand(
                request.slug(),
                request.title(),
                request.summary(),
                request.description(),
                request.sampleInput(),
                request.sampleOutput(),
                request.topic(),
                request.difficulty(),
                request.allowedLanguages(),
                request.starterCodes(),
                request.testCases() == null ? null : request.testCases().stream()
                        .map(testCase -> new CreateProblemTestCaseCommand(
                                testCase.input(), testCase.expectedOutput(), testCase.timeLimitMillis()))
                        .toList()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ProgrammingProblemDetailResponse(
                problem.id(), problem.slug(), problem.title(), problem.summary(), problem.description(),
                problem.sampleInput(), problem.sampleOutput(), problem.topic(), problem.difficulty(),
                problem.allowedLanguages(), problem.starterCodes(), problem.createdAt()
        ));
    }
}
