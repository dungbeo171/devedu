package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.exception.CourseManagementForbiddenException;
import com.devedu.learningplatform.application.port.in.command.CompleteLessonCommand;
import com.devedu.learningplatform.application.port.in.command.CreateCourseCommand;
import com.devedu.learningplatform.application.port.in.command.CreateCourseTopicCommand;
import com.devedu.learningplatform.application.port.in.command.CreateLessonCommand;
import com.devedu.learningplatform.application.port.in.command.SetLessonVideoCommand;
import com.devedu.learningplatform.application.port.out.CourseRepository;
import com.devedu.learningplatform.application.port.out.CourseTopicRepository;
import com.devedu.learningplatform.application.port.out.LessonProgressRepository;
import com.devedu.learningplatform.application.port.out.LessonRepository;
import com.devedu.learningplatform.domain.model.Course;
import com.devedu.learningplatform.domain.model.CourseTopic;
import com.devedu.learningplatform.domain.model.Lesson;
import com.devedu.learningplatform.domain.model.LessonProgress;
import com.devedu.learningplatform.domain.model.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseLearningServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-22T10:00:00Z");
    private static final UUID TEACHER = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_TEACHER = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID STUDENT = UUID.fromString("20000000-0000-0000-0000-000000000003");

    private final TestCourseRepository courseRepository = new TestCourseRepository();
    private final TestTopicRepository topicRepository = new TestTopicRepository();
    private final TestLessonRepository lessonRepository = new TestLessonRepository();
    private final TestProgressRepository progressRepository = new TestProgressRepository();
    private final CourseLearningService service = new CourseLearningService(courseRepository, topicRepository,
            lessonRepository, progressRepository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void teacherCreatesCourseContentAndPublishesVideoUrl() {
        var course = service.createCourse(new CreateCourseCommand(TEACHER, UserRole.TEACHER,
                "java-core", "Java Core", "Java nền tảng"));
        var topic = service.createTopic(new CreateCourseTopicCommand(TEACHER, UserRole.TEACHER,
                course.id(), "Cú pháp", 1));
        var lesson = service.createLesson(new CreateLessonCommand(TEACHER, UserRole.TEACHER,
                topic.id(), "Biến", "Nội dung lesson", 1));
        var withVideo = service.setLessonVideo(new SetLessonVideoCommand(TEACHER, UserRole.TEACHER,
                lesson.id(), "https://cdn.example.com/java.mp4"));

        assertThat(withVideo.videoUrl()).isEqualTo("https://cdn.example.com/java.mp4");
        assertThat(service.getCourseBySlug("JAVA-CORE").topics().get(0).lessons())
                .extracting(Lesson::id).containsExactly(lesson.id());
    }

    @Test
    void studentCannotCreateCourse() {
        assertThatThrownBy(() -> service.createCourse(new CreateCourseCommand(STUDENT, UserRole.STUDENT,
                "java-core", "Java Core", "Java nền tảng")))
                .isInstanceOf(CourseManagementForbiddenException.class);
    }

    @Test
    void teacherCannotManageAnotherTeachersCourse() {
        var course = service.createCourse(new CreateCourseCommand(TEACHER, UserRole.TEACHER,
                "java-core", "Java Core", "Java nền tảng"));
        assertThatThrownBy(() -> service.createTopic(new CreateCourseTopicCommand(
                OTHER_TEACHER, UserRole.TEACHER, course.id(), "Không hợp lệ", 1)))
                .isInstanceOf(CourseManagementForbiddenException.class);
    }

    @Test
    void rejectsNonHttpVideoUrl() {
        var lesson = createLesson();
        assertThatThrownBy(() -> service.setLessonVideo(new SetLessonVideoCommand(
                TEACHER, UserRole.TEACHER, lesson.id(), "javascript:alert(1)")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTP");
    }

    @Test
    void completingLessonIsIdempotent() {
        var lesson = createLesson();
        var first = service.completeLesson(new CompleteLessonCommand(STUDENT, lesson.id()));
        var second = service.completeLesson(new CompleteLessonCommand(STUDENT, lesson.id()));
        assertThat(second).isEqualTo(first);
        assertThat(progressRepository.progresses).hasSize(1);
    }

    @Test
    void rejectsCourseTitleLongerThanDatabaseColumn() {
        assertThatThrownBy(() -> service.createCourse(new CreateCourseCommand(
                TEACHER, UserRole.TEACHER, "java-core", "x".repeat(181), "Java nền tảng")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("180");
    }

    private Lesson createLesson() {
        var course = service.createCourse(new CreateCourseCommand(TEACHER, UserRole.TEACHER,
                "java-core", "Java Core", "Java nền tảng"));
        var topic = service.createTopic(new CreateCourseTopicCommand(TEACHER, UserRole.TEACHER,
                course.id(), "Cú pháp", 1));
        return service.createLesson(new CreateLessonCommand(TEACHER, UserRole.TEACHER,
                topic.id(), "Biến", "Nội dung", 1));
    }

    private static final class TestCourseRepository implements CourseRepository {
        private final Map<UUID, Course> courses = new HashMap<>();
        @Override public boolean existsBySlug(String slug) { return courses.values().stream().anyMatch(c -> c.slug().equals(slug)); }
        @Override public Course save(Course course) { courses.put(course.id(), course); return course; }
        @Override public List<Course> findAll() { return courses.values().stream().sorted(Comparator.comparing(Course::title)).toList(); }
        @Override public Optional<Course> findById(UUID id) { return Optional.ofNullable(courses.get(id)); }
        @Override public Optional<Course> findBySlug(String slug) { return courses.values().stream().filter(c -> c.slug().equals(slug)).findFirst(); }
    }

    private static final class TestTopicRepository implements CourseTopicRepository {
        private final Map<UUID, CourseTopic> topics = new HashMap<>();
        @Override public CourseTopic save(CourseTopic topic) { topics.put(topic.id(), topic); return topic; }
        @Override public Optional<CourseTopic> findById(UUID id) { return Optional.ofNullable(topics.get(id)); }
        @Override public List<CourseTopic> findAllByCourseId(UUID courseId) { return topics.values().stream().filter(t -> t.courseId().equals(courseId)).sorted(Comparator.comparingInt(CourseTopic::position)).toList(); }
    }

    private static final class TestLessonRepository implements LessonRepository {
        private final Map<UUID, Lesson> lessons = new HashMap<>();
        @Override public Lesson save(Lesson lesson) { lessons.put(lesson.id(), lesson); return lesson; }
        @Override public Optional<Lesson> findById(UUID id) { return Optional.ofNullable(lessons.get(id)); }
        @Override public List<Lesson> findAllByTopicIds(List<UUID> topicIds) { return lessons.values().stream().filter(l -> topicIds.contains(l.topicId())).sorted(Comparator.comparingInt(Lesson::position)).toList(); }
    }

    private static final class TestProgressRepository implements LessonProgressRepository {
        private final List<LessonProgress> progresses = new ArrayList<>();
        @Override public Optional<LessonProgress> findByStudentIdAndLessonId(UUID studentId, UUID lessonId) { return progresses.stream().filter(p -> p.studentId().equals(studentId) && p.lessonId().equals(lessonId)).findFirst(); }
        @Override public LessonProgress save(LessonProgress progress) { progresses.add(progress); return progress; }
    }
}
