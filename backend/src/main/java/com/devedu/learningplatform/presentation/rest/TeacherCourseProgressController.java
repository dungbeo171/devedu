package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.port.in.CourseClassroomUseCase;
import com.devedu.learningplatform.application.port.in.command.ManageCourseCommand;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.presentation.rest.dto.CourseStudentProgressResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teacher/courses/{courseId}/student-progress")
public class TeacherCourseProgressController {
    private final CourseClassroomUseCase useCase;

    public TeacherCourseProgressController(CourseClassroomUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public List<CourseStudentProgressResponse> list(@PathVariable UUID courseId,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return useCase.listStudentProgress(new ManageCourseCommand(actor.id(), actor.role(), courseId))
                .stream().map(CourseStudentProgressResponse::from).toList();
    }
}
