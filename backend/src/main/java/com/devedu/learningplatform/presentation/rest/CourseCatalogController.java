package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.port.in.CourseLearningUseCase;
import com.devedu.learningplatform.presentation.rest.dto.CourseDetailResponse;
import com.devedu.learningplatform.presentation.rest.dto.CourseResponse;
import com.devedu.learningplatform.presentation.rest.dto.LessonResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CourseCatalogController {
    private final CourseLearningUseCase useCase;
    public CourseCatalogController(CourseLearningUseCase useCase) { this.useCase = useCase; }

    @GetMapping("/courses")
    public List<CourseResponse> listCourses() {
        return useCase.listCourses().stream().map(CourseResponse::from).toList();
    }

    @GetMapping("/courses/{slug}")
    public CourseDetailResponse getCourse(@PathVariable String slug) {
        return CourseDetailResponse.from(useCase.getCourseBySlug(slug));
    }

    @GetMapping("/lessons/{lessonId}")
    public LessonResponse getLesson(@PathVariable UUID lessonId) {
        return LessonResponse.from(useCase.getLesson(lessonId));
    }
}
