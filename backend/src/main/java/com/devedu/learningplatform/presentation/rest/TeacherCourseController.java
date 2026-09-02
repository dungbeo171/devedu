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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.multipart.MultipartFile;
import com.devedu.learningplatform.application.port.in.command.ManageCourseCommand;
import com.devedu.learningplatform.application.port.in.command.EnrollCourseStudentsCommand;
import com.devedu.learningplatform.application.port.in.command.UploadCourseMaterialCommand;
import com.devedu.learningplatform.presentation.rest.dto.EnrollStudentRequest;
import com.devedu.learningplatform.presentation.rest.dto.CourseStudentResponse;
import com.devedu.learningplatform.presentation.rest.dto.CourseMaterialResponse;
import com.devedu.learningplatform.presentation.rest.dto.ManagedCourseResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import java.util.UUID;
import com.devedu.learningplatform.application.port.in.command.ManageCourseStudentsCommand;
import com.devedu.learningplatform.presentation.rest.dto.ManageCourseStudentsRequest;
import com.devedu.learningplatform.presentation.rest.dto.CourseStudentCandidateResponse;
import com.devedu.learningplatform.application.port.in.command.UpdateCourseStudentCommand;
import com.devedu.learningplatform.presentation.rest.dto.UpdateCourseStudentRequest;

@RestController
@RequestMapping("/api/teacher")
public class TeacherCourseController {
    private final CourseLearningUseCase useCase;
    public TeacherCourseController(CourseLearningUseCase useCase) { this.useCase = useCase; }

    @PostMapping("/courses")
    public ResponseEntity<CourseResponse> createCourse(@RequestBody CreateCourseRequest request,
                                                        @AuthenticationPrincipal AuthenticatedUser actor) {
        var course = useCase.createCourse(new CreateCourseCommand(actor.id(), actor.role(), request.slug(),
                request.title(), request.description(), request.startDate(), request.endDate()));
        return ResponseEntity.status(HttpStatus.CREATED).body(CourseResponse.from(course));
    }

    @GetMapping("/courses")
    public List<ManagedCourseResponse> listManagedCourses(@AuthenticationPrincipal AuthenticatedUser actor) {
        return useCase.listManagedCourses(actor.id(), actor.role()).stream()
                .map(ManagedCourseResponse::from).toList();
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

    @GetMapping("/courses/{courseId}/students")
    public List<CourseStudentResponse> listStudents(@PathVariable UUID courseId,
                                                     @AuthenticationPrincipal AuthenticatedUser actor) {
        return useCase.listEnrolledStudents(new ManageCourseCommand(actor.id(), actor.role(), courseId))
                .stream().map(CourseStudentResponse::from).toList();
    }

    @GetMapping("/courses/{courseId}/student-candidates")
    public List<CourseStudentCandidateResponse> searchStudentCandidates(@PathVariable UUID courseId,
            @RequestParam(name = "q", defaultValue = "") String query,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return useCase.searchAvailableStudents(new ManageCourseCommand(actor.id(), actor.role(), courseId), query)
                .stream().map(CourseStudentCandidateResponse::from).toList();
    }

    @PostMapping("/courses/{courseId}/students/bulk")
    public List<CourseStudentResponse> addStudents(@PathVariable UUID courseId,
            @RequestBody ManageCourseStudentsRequest request, @AuthenticationPrincipal AuthenticatedUser actor) {
        return useCase.addStudents(new ManageCourseStudentsCommand(
                        actor.id(), actor.role(), courseId, request.studentIds()))
                .stream().map(CourseStudentResponse::from).toList();
    }

    @DeleteMapping("/courses/{courseId}/students")
    public List<CourseStudentResponse> removeStudents(@PathVariable UUID courseId,
            @RequestBody ManageCourseStudentsRequest request, @AuthenticationPrincipal AuthenticatedUser actor) {
        return useCase.removeStudents(new ManageCourseStudentsCommand(
                        actor.id(), actor.role(), courseId, request.studentIds()))
                .stream().map(CourseStudentResponse::from).toList();
    }

    @PutMapping("/courses/{courseId}/students/{studentId}")
    public List<CourseStudentResponse> updateStudent(@PathVariable UUID courseId, @PathVariable long studentId,
            @RequestBody UpdateCourseStudentRequest request, @AuthenticationPrincipal AuthenticatedUser actor) {
        return useCase.updateStudent(new UpdateCourseStudentCommand(actor.id(), actor.role(), courseId,
                        studentId, request.displayName())).stream().map(CourseStudentResponse::from).toList();
    }

    @PostMapping("/courses/{courseId}/students")
    public List<CourseStudentResponse> enrollStudent(@PathVariable UUID courseId,
            @RequestBody EnrollStudentRequest request, @AuthenticationPrincipal AuthenticatedUser actor) {
        return useCase.enrollStudents(new EnrollCourseStudentsCommand(
                        actor.id(), actor.role(), courseId, request.studentCode()))
                .stream().map(CourseStudentResponse::from).toList();
    }

    @PostMapping(value = "/courses/{courseId}/students/import", consumes = "multipart/form-data")
    public List<CourseStudentResponse> importStudents(@PathVariable UUID courseId,
            @RequestParam("file") MultipartFile file, @AuthenticationPrincipal AuthenticatedUser actor)
            throws java.io.IOException {
        var fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!fileName.endsWith(".txt")) throw new IllegalArgumentException("Student import file must be TXT");
        if (file.getSize() > 1_048_576) throw new IllegalArgumentException("Student import file must not exceed 1 MB");
        var text = new String(file.getBytes(), StandardCharsets.UTF_8).replace("\uFEFF", "");
        return useCase.enrollStudents(new EnrollCourseStudentsCommand(actor.id(), actor.role(), courseId, text))
                .stream().map(CourseStudentResponse::from).toList();
    }

    @PostMapping(value = "/courses/{courseId}/materials", consumes = "multipart/form-data")
    public ResponseEntity<CourseMaterialResponse> uploadMaterial(@PathVariable UUID courseId,
            @RequestParam("title") String title, @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser actor) throws java.io.IOException {
        var material = useCase.uploadMaterial(new UploadCourseMaterialCommand(
                actor.id(), actor.role(), courseId, title, file.getOriginalFilename(), file.getBytes()));
        return ResponseEntity.status(HttpStatus.CREATED).body(CourseMaterialResponse.from(material));
    }
}
