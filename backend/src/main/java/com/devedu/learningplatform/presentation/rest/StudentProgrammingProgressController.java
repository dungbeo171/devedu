package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.port.in.ListSolvedProgrammingProblemsUseCase;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.presentation.rest.dto.SolvedProgrammingProblemsResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/problem-progress")
public class StudentProgrammingProgressController {

    private final ListSolvedProgrammingProblemsUseCase useCase;

    public StudentProgrammingProgressController(ListSolvedProgrammingProblemsUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public SolvedProgrammingProblemsResponse getProgress(@AuthenticationPrincipal AuthenticatedUser student) {
        return new SolvedProgrammingProblemsResponse(useCase.listSolvedProblemIds(student.id()));
    }
}
