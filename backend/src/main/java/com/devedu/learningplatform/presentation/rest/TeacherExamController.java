package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.port.in.ExamUseCase;
import com.devedu.learningplatform.application.port.in.command.AddExamQuestionCommand;
import com.devedu.learningplatform.application.port.in.command.CreateExamCommand;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.presentation.rest.dto.*;
import org.springframework.http.HttpStatus; import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List; import java.util.UUID;

@RestController @RequestMapping("/api/teacher/exams")
public class TeacherExamController {
    private final ExamUseCase useCase;
    public TeacherExamController(ExamUseCase useCase){this.useCase=useCase;}
    @GetMapping public List<ExamResponse> list(@AuthenticationPrincipal AuthenticatedUser actor){return useCase.listManagedExams(actor.id(),actor.role()).stream().map(ExamResponse::from).toList();}
    @PostMapping public ResponseEntity<ExamResponse> create(@RequestBody CreateExamRequest request,@AuthenticationPrincipal AuthenticatedUser actor){var exam=useCase.createExam(new CreateExamCommand(actor.id(),actor.role(),request.slug(),request.title(),request.description(),request.scheduledAt(),request.durationMinutes()));return ResponseEntity.status(HttpStatus.CREATED).body(ExamResponse.from(exam));}
    @PostMapping("/{examId}/questions") public ResponseEntity<TeacherExamQuestionResponse> addQuestion(@PathVariable UUID examId,@RequestBody AddExamQuestionRequest request,@AuthenticationPrincipal AuthenticatedUser actor){var q=useCase.addQuestion(new AddExamQuestionCommand(actor.id(),actor.role(),examId,request.type(),request.prompt(),request.options(),request.correctOptionIndex(),request.codingLanguage(),request.points(),request.position()));return ResponseEntity.status(HttpStatus.CREATED).body(TeacherExamQuestionResponse.from(q));}
    @GetMapping("/{examId}/results") public List<TeacherExamResultResponse> results(@PathVariable UUID examId,@AuthenticationPrincipal AuthenticatedUser actor){return useCase.listResults(actor.id(),actor.role(),examId).stream().map(TeacherExamResultResponse::from).toList();}
}
