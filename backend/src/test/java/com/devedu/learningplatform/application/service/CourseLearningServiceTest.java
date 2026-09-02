package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.exception.CourseManagementForbiddenException;
import com.devedu.learningplatform.application.port.in.command.CompleteLessonCommand;
import com.devedu.learningplatform.application.port.in.command.CreateCourseCommand;
import com.devedu.learningplatform.application.port.in.command.CreateCourseTopicCommand;
import com.devedu.learningplatform.application.port.in.command.CreateLessonCommand;
import com.devedu.learningplatform.application.port.in.command.SetLessonVideoCommand;
import com.devedu.learningplatform.application.port.in.command.EnrollCourseStudentsCommand;
import com.devedu.learningplatform.application.port.in.command.UploadCourseMaterialCommand;
import com.devedu.learningplatform.application.port.in.command.AccessCourseMaterialsCommand;
import com.devedu.learningplatform.application.port.in.command.ManageCourseCommand;
import com.devedu.learningplatform.application.port.in.command.ManageCourseStudentsCommand;
import com.devedu.learningplatform.application.port.out.CourseRepository;
import com.devedu.learningplatform.application.port.out.CourseTopicRepository;
import com.devedu.learningplatform.application.port.out.LessonProgressRepository;
import com.devedu.learningplatform.application.port.out.LessonRepository;
import com.devedu.learningplatform.application.port.out.CourseEnrollmentRepository;
import com.devedu.learningplatform.application.port.out.CourseMaterialRepository;
import com.devedu.learningplatform.application.port.out.CourseFileStorage;
import com.devedu.learningplatform.application.port.out.UserRepository;
import com.devedu.learningplatform.domain.model.Course;
import com.devedu.learningplatform.domain.model.CourseTopic;
import com.devedu.learningplatform.domain.model.Lesson;
import com.devedu.learningplatform.domain.model.LessonProgress;
import com.devedu.learningplatform.domain.model.UserRole;
import com.devedu.learningplatform.domain.model.User;
import com.devedu.learningplatform.domain.model.CourseEnrollment;
import com.devedu.learningplatform.domain.model.CourseMaterial;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
    private final TestEnrollmentRepository enrollmentRepository = new TestEnrollmentRepository();
    private final TestMaterialRepository materialRepository = new TestMaterialRepository();
    private final TestFileStorage fileStorage = new TestFileStorage();
    private final TestUserRepository userRepository = new TestUserRepository();
    private final CourseLearningService service = new CourseLearningService(courseRepository, topicRepository,
            lessonRepository, progressRepository, enrollmentRepository, materialRepository, fileStorage,
            userRepository, Clock.fixed(NOW, ZoneOffset.UTC));

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

    @Test
    void teacherEnrollsStudentByCodeAndStudentCanAccessUploadedMaterial() {
        var course = service.createCourse(new CreateCourseCommand(TEACHER, UserRole.TEACHER,
                "java-core", "Java Core", "Java nền tảng"));

        var roster = service.enrollStudents(new EnrollCourseStudentsCommand(
                TEACHER, UserRole.TEACHER, course.id(), "SV000003"));
        var material = service.uploadMaterial(new UploadCourseMaterialCommand(
                TEACHER, UserRole.TEACHER, course.id(), "Slide", "slide.pdf", new byte[]{1, 2, 3}));

        assertThat(roster).extracting(item -> item.user().studentCode()).containsExactly("SV000003");
        assertThat(service.listMaterials(new AccessCourseMaterialsCommand(STUDENT, UserRole.STUDENT, course.id())))
                .containsExactly(material);
        assertThat(fileStorage.files).containsKey(material.storageKey());
    }

    @Test
    void managedCourseListContainsOnlyTeachersClassesWithCountAndStatus() {
        var active = service.createCourse(new CreateCourseCommand(TEACHER, UserRole.TEACHER,
                "java-2026", "Java 2026", "", LocalDate.of(2026, 8, 1), null));
        service.createCourse(new CreateCourseCommand(TEACHER, UserRole.TEACHER,
                "java-2025", "Java 2025", "", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)));
        service.createCourse(new CreateCourseCommand(OTHER_TEACHER, UserRole.TEACHER,
                "cpp-2026", "C++ 2026", "", LocalDate.of(2026, 8, 1), null));
        service.enrollStudents(new EnrollCourseStudentsCommand(
                TEACHER, UserRole.TEACHER, active.id(), "SV000003"));

        var managed = service.listManagedCourses(TEACHER, UserRole.TEACHER);

        assertThat(managed).hasSize(2);
        assertThat(managed).filteredOn(item -> item.course().id().equals(active.id())).singleElement()
                .satisfies(item -> {
                    assertThat(item.studentCount()).isEqualTo(1);
                    assertThat(item.status()).isEqualTo(com.devedu.learningplatform.domain.model.CourseStatus.ACTIVE);
                });
        assertThat(managed).filteredOn(item -> item.course().slug().equals("java-2025")).singleElement()
                .satisfies(item -> assertThat(item.status())
                        .isEqualTo(com.devedu.learningplatform.domain.model.CourseStatus.ENDED));
    }

    @Test
    void teacherSearchesAddsAndRemovesStudentsFromOwnedCourse() {
        var course = service.createCourse(new CreateCourseCommand(TEACHER, UserRole.TEACHER,
                "java-members", "Java Members", ""));

        assertThat(service.searchAvailableStudents(
                new ManageCourseCommand(TEACHER, UserRole.TEACHER, course.id()), "SV000003"))
                .extracting(User::publicId).containsExactly(3L);

        var added = service.addStudents(new ManageCourseStudentsCommand(
                TEACHER, UserRole.TEACHER, course.id(), List.of(3L)));
        assertThat(added).extracting(item -> item.user().studentCode()).containsExactly("SV000003");
        assertThat(added.get(0).enrolledAt()).isEqualTo(NOW);

        var removed = service.removeStudents(new ManageCourseStudentsCommand(
                TEACHER, UserRole.TEACHER, course.id(), List.of(3L)));
        assertThat(removed).isEmpty();
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

    private static final class TestEnrollmentRepository implements CourseEnrollmentRepository {
        private final List<CourseEnrollment> values = new ArrayList<>();
        @Override public boolean existsByCourseIdAndStudentId(UUID courseId, UUID studentId) { return values.stream().anyMatch(value -> value.courseId().equals(courseId) && value.studentId().equals(studentId)); }
        @Override public List<CourseEnrollment> saveAll(List<CourseEnrollment> enrollments) { values.addAll(enrollments); return enrollments; }
        @Override public List<UUID> findStudentIdsByCourseId(UUID courseId) { return values.stream().filter(value -> value.courseId().equals(courseId)).map(CourseEnrollment::studentId).toList(); }
        @Override public List<CourseEnrollment> findAllByCourseId(UUID courseId) { return values.stream().filter(value -> value.courseId().equals(courseId)).toList(); }
        @Override public void deleteByCourseIdAndStudentIds(UUID courseId, List<UUID> studentIds) { values.removeIf(value -> value.courseId().equals(courseId) && studentIds.contains(value.studentId())); }
    }

    private static final class TestMaterialRepository implements CourseMaterialRepository {
        private final Map<UUID, CourseMaterial> values = new HashMap<>();
        @Override public CourseMaterial save(CourseMaterial material) { values.put(material.id(), material); return material; }
        @Override public Optional<CourseMaterial> findById(UUID id) { return Optional.ofNullable(values.get(id)); }
        @Override public List<CourseMaterial> findAllByCourseId(UUID courseId) { return values.values().stream().filter(value -> value.courseId().equals(courseId)).toList(); }
    }

    private static final class TestFileStorage implements CourseFileStorage {
        private final Map<String, byte[]> files = new HashMap<>();
        @Override public void store(String storageKey, byte[] content) { files.put(storageKey, content.clone()); }
        @Override public byte[] load(String storageKey) { return files.get(storageKey).clone(); }
        @Override public void delete(String storageKey) { files.remove(storageKey); }
    }

    private static final class TestUserRepository implements UserRepository {
        private final Map<UUID, User> users = new HashMap<>();
        private TestUserRepository() {
            users.put(STUDENT, new User(STUDENT, 3, "SV000003", null, "Student", "student@example.com",
                    "hash", UserRole.STUDENT, NOW));
        }
        @Override public boolean existsByEmail(String email) { return users.values().stream().anyMatch(user -> user.email().equals(email)); }
        @Override public Optional<User> findByEmail(String email) { return users.values().stream().filter(user -> user.email().equals(email)).findFirst(); }
        @Override public Optional<User> findById(UUID id) { return Optional.ofNullable(users.get(id)); }
        @Override public Optional<User> findByPublicId(long publicId) { return users.values().stream().filter(user -> user.publicId() == publicId).findFirst(); }
        @Override public Optional<User> findByStudentCode(String code) { return users.values().stream().filter(user -> code.equals(user.studentCode())).findFirst(); }
        @Override public List<User> findAll() { return List.copyOf(users.values()); }
        @Override public User save(User user) { users.put(user.id(), user); return user; }
        @Override public void deleteById(UUID id) { users.remove(id); }
    }
}
