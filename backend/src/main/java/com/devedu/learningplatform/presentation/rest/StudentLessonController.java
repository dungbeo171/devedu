package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.port.in.CourseLearningUseCase;
import com.devedu.learningplatform.application.port.in.command.CompleteLessonCommand;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.presentation.rest.dto.LessonProgressResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/student/lessons")
public class StudentLessonController {
    private final CourseLearningUseCase useCase;
    public StudentLessonController(CourseLearningUseCase useCase) { this.useCase = useCase; }

    @PostMapping("/{lessonId}/complete")
    public LessonProgressResponse complete(@PathVariable UUID lessonId,
                                            @AuthenticationPrincipal AuthenticatedUser student) {
        return LessonProgressResponse.from(useCase.completeLesson(new CompleteLessonCommand(student.id(), lessonId)));
    }
}
