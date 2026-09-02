package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.port.in.CourseClassroomUseCase;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.presentation.rest.dto.CourseProblemResponse;
import com.devedu.learningplatform.presentation.rest.dto.StudentCourseDetailsResponse;
import com.devedu.learningplatform.presentation.rest.dto.StudentCourseResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/student/courses")
public class StudentCoursesController {
    private final CourseClassroomUseCase useCase;
    public StudentCoursesController(CourseClassroomUseCase useCase) { this.useCase = useCase; }
    @GetMapping
    public List<StudentCourseResponse> list(@AuthenticationPrincipal AuthenticatedUser student) {
        return useCase.listStudentCourses(student.id()).stream().map(StudentCourseResponse::from).toList();
    }
    @GetMapping("/{courseId}")
    public StudentCourseDetailsResponse details(@PathVariable UUID courseId,
            @AuthenticationPrincipal AuthenticatedUser student) {
        var details = useCase.getStudentCourse(student.id(), courseId);
        return new StudentCourseDetailsResponse(StudentCourseResponse.from(details.summary()),
                details.problems().stream().map(CourseProblemResponse::from).toList());
    }
}
