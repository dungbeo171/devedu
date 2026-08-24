package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.port.in.CourseLearningUseCase;
import com.devedu.learningplatform.application.port.in.command.CreateCourseCommand;
import com.devedu.learningplatform.application.port.in.command.CreateCourseTopicCommand;
import com.devedu.learningplatform.application.port.in.command.CreateLessonCommand;
import com.devedu.learningplatform.application.port.in.command.SetLessonVideoCommand;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.presentation.rest.dto.CourseResponse;
import com.devedu.learningplatform.presentation.rest.dto.CourseTopicCreatedResponse;
import com.devedu.learningplatform.presentation.rest.dto.CreateCourseRequest;
import com.devedu.learningplatform.presentation.rest.dto.CreateCourseTopicRequest;
import com.devedu.learningplatform.presentation.rest.dto.CreateLessonRequest;
import com.devedu.learningplatform.presentation.rest.dto.LessonResponse;
import com.devedu.learningplatform.presentation.rest.dto.SetLessonVideoRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/teacher")
public class TeacherCourseController {
    private final CourseLearningUseCase useCase;
    public TeacherCourseController(CourseLearningUseCase useCase) { this.useCase = useCase; }

    @PostMapping("/courses")
    public ResponseEntity<CourseResponse> createCourse(@RequestBody CreateCourseRequest request,
                                                        @AuthenticationPrincipal AuthenticatedUser actor) {
        var course = useCase.createCourse(new CreateCourseCommand(actor.id(), actor.role(), request.slug(), request.title(), request.description()));
        return ResponseEntity.status(HttpStatus.CREATED).body(CourseResponse.from(course));
    }

    @PostMapping("/courses/{courseId}/topics")
    public ResponseEntity<CourseTopicCreatedResponse> createTopic(@PathVariable UUID courseId,
            @RequestBody CreateCourseTopicRequest request, @AuthenticationPrincipal AuthenticatedUser actor) {
        var topic = useCase.createTopic(new CreateCourseTopicCommand(actor.id(), actor.role(), courseId, request.title(), request.position()));
        return ResponseEntity.status(HttpStatus.CREATED).body(CourseTopicCreatedResponse.from(topic));
    }

    @PostMapping("/topics/{topicId}/lessons")
    public ResponseEntity<LessonResponse> createLesson(@PathVariable UUID topicId,
            @RequestBody CreateLessonRequest request, @AuthenticationPrincipal AuthenticatedUser actor) {
        var lesson = useCase.createLesson(new CreateLessonCommand(actor.id(), actor.role(), topicId, request.title(), request.content(), request.position()));
        return ResponseEntity.status(HttpStatus.CREATED).body(LessonResponse.from(lesson));
    }

    @PutMapping("/lessons/{lessonId}/video")
    public LessonResponse setVideo(@PathVariable UUID lessonId, @RequestBody SetLessonVideoRequest request,
                                   @AuthenticationPrincipal AuthenticatedUser actor) {
        return LessonResponse.from(useCase.setLessonVideo(new SetLessonVideoCommand(actor.id(), actor.role(), lessonId, request.videoUrl())));
    }
}
