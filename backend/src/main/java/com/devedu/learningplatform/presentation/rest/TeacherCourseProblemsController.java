package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.port.in.CourseClassroomUseCase;
import com.devedu.learningplatform.application.port.in.command.ManageCourseCommand;
import com.devedu.learningplatform.application.port.in.command.ManageCourseProblemCommand;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.presentation.rest.dto.AssignCourseProblemRequest;
import com.devedu.learningplatform.presentation.rest.dto.CourseProblemResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teacher/courses/{courseId}/problems")
public class TeacherCourseProblemsController {
    private final CourseClassroomUseCase useCase;
    public TeacherCourseProblemsController(CourseClassroomUseCase useCase) { this.useCase = useCase; }
    @GetMapping
    public List<CourseProblemResponse> list(@PathVariable UUID courseId,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return useCase.listManagedProblems(new ManageCourseCommand(actor.id(), actor.role(), courseId))
                .stream().map(CourseProblemResponse::from).toList();
    }
    @PostMapping
    public List<CourseProblemResponse> assign(@PathVariable UUID courseId,
            @RequestBody AssignCourseProblemRequest request, @AuthenticationPrincipal AuthenticatedUser actor) {
        return useCase.assignProblem(new ManageCourseProblemCommand(actor.id(), actor.role(), courseId, request.problemId()))
                .stream().map(CourseProblemResponse::from).toList();
    }
    @DeleteMapping("/{problemId}")
    public List<CourseProblemResponse> remove(@PathVariable UUID courseId, @PathVariable UUID problemId,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return useCase.removeProblem(new ManageCourseProblemCommand(actor.id(), actor.role(), courseId, problemId))
                .stream().map(CourseProblemResponse::from).toList();
    }
}
