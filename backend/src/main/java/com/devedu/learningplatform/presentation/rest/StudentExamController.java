package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.port.in.ExamUseCase;
import com.devedu.learningplatform.application.port.in.command.AnswerExamQuestionCommand;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.presentation.rest.dto.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List; import java.util.UUID;

@RestController @RequestMapping("/api/exams")
public class StudentExamController {
    private final ExamUseCase useCase;
    public StudentExamController(ExamUseCase useCase){this.useCase=useCase;}
    @GetMapping public List<ExamResponse> list(){return useCase.listExams().stream().map(ExamResponse::from).toList();}
    @GetMapping("/{slug}") public ExamResponse detail(@PathVariable String slug){return ExamResponse.from(useCase.getExamBySlug(slug).exam());}
    @PostMapping("/{slug}/attempts") public ExamAttemptSessionResponse start(@PathVariable String slug,@AuthenticationPrincipal AuthenticatedUser student){return ExamAttemptSessionResponse.from(useCase.startExam(student.id(),slug));}
    @GetMapping("/attempts/{attemptId}") public ExamAttemptSessionResponse attempt(@PathVariable UUID attemptId,@AuthenticationPrincipal AuthenticatedUser student){return ExamAttemptSessionResponse.from(useCase.getAttempt(student.id(),attemptId));}
    @PutMapping("/attempts/{attemptId}/answers/{questionId}") public ExamAnswerResponse answer(@PathVariable UUID attemptId,@PathVariable UUID questionId,@RequestBody AnswerExamQuestionRequest request,@AuthenticationPrincipal AuthenticatedUser student){return ExamAnswerResponse.from(useCase.answerQuestion(new AnswerExamQuestionCommand(student.id(),attemptId,questionId,request.selectedOptionIndex(),request.sourceCode())));}
    @PostMapping("/attempts/{attemptId}/submit") public ExamResultResponse submit(@PathVariable UUID attemptId,@AuthenticationPrincipal AuthenticatedUser student){return ExamResultResponse.from(useCase.submitExam(student.id(),attemptId));}
    @GetMapping("/attempts/{attemptId}/result") public ExamResultResponse result(@PathVariable UUID attemptId,@AuthenticationPrincipal AuthenticatedUser student){return ExamResultResponse.from(useCase.getResult(student.id(),attemptId));}
}
