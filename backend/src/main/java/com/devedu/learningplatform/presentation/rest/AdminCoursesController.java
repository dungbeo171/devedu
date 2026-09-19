package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.port.in.CourseLearningUseCase;
import com.devedu.learningplatform.application.port.in.command.DeleteCourseCommand;
import com.devedu.learningplatform.application.port.in.command.UpdateCourseCommand;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.presentation.rest.dto.CourseResponse;
import com.devedu.learningplatform.presentation.rest.dto.UpdateCourseRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/courses")
public class AdminCoursesController {

    private final CourseLearningUseCase useCase;

    public AdminCoursesController(CourseLearningUseCase useCase) {
        this.useCase = useCase;
    }

    @PutMapping("/{courseId}")
    public CourseResponse updateCourse(
            @PathVariable UUID courseId,
            @RequestBody UpdateCourseRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        return CourseResponse.from(useCase.updateCourse(new UpdateCourseCommand(
                actor.id(), actor.role(), courseId, request.slug(), request.title(), request.description(),
                request.startDate(), request.endDate()
        )));
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        useCase.deleteCourse(new DeleteCourseCommand(actor.id(), actor.role(), courseId));
        return ResponseEntity.noContent().build();
    }
}
