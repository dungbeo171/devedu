package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.exception.CourseManagementForbiddenException;
import com.devedu.learningplatform.application.exception.CourseResourceNotFoundException;
import com.devedu.learningplatform.application.exception.CourseSlugAlreadyExistsException;
import com.devedu.learningplatform.application.port.in.CourseLearningUseCase;
import com.devedu.learningplatform.application.port.in.command.CompleteLessonCommand;
import com.devedu.learningplatform.application.port.in.command.CreateCourseCommand;
import com.devedu.learningplatform.application.port.in.command.CreateCourseTopicCommand;
import com.devedu.learningplatform.application.port.in.command.CreateLessonCommand;
import com.devedu.learningplatform.application.port.in.command.SetLessonVideoCommand;
import com.devedu.learningplatform.application.port.in.result.CourseDetails;
import com.devedu.learningplatform.application.port.in.result.CourseTopicDetails;
import com.devedu.learningplatform.application.port.out.CourseRepository;
import com.devedu.learningplatform.application.port.out.CourseTopicRepository;
import com.devedu.learningplatform.application.port.out.LessonProgressRepository;
import com.devedu.learningplatform.application.port.out.LessonRepository;
import com.devedu.learningplatform.application.port.out.CourseEnrollmentRepository;
import com.devedu.learningplatform.application.port.out.CourseMaterialRepository;
import com.devedu.learningplatform.application.port.out.CourseFileStorage;
import com.devedu.learningplatform.application.port.out.UserRepository;
import com.devedu.learningplatform.application.port.in.command.ManageCourseCommand;
import com.devedu.learningplatform.application.port.in.command.EnrollCourseStudentsCommand;
import com.devedu.learningplatform.application.port.in.command.UploadCourseMaterialCommand;
import com.devedu.learningplatform.application.port.in.command.AccessCourseMaterialsCommand;
import com.devedu.learningplatform.application.port.in.command.DownloadCourseMaterialCommand;
import com.devedu.learningplatform.application.port.in.result.CourseMaterialContent;
import com.devedu.learningplatform.domain.model.CourseEnrollment;
import com.devedu.learningplatform.domain.model.CourseMaterial;
import com.devedu.learningplatform.domain.model.User;
import com.devedu.learningplatform.domain.model.Course;
import com.devedu.learningplatform.domain.model.CourseTopic;
import com.devedu.learningplatform.domain.model.Lesson;
import com.devedu.learningplatform.domain.model.LessonProgress;
import com.devedu.learningplatform.domain.model.UserRole;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Locale;
import java.util.LinkedHashSet;
import java.util.Map;
import com.devedu.learningplatform.application.port.in.result.ManagedCourse;
import com.devedu.learningplatform.application.port.in.result.EnrolledCourseStudent;
import com.devedu.learningplatform.application.port.in.command.ManageCourseStudentsCommand;
import com.devedu.learningplatform.application.port.in.command.UpdateCourseStudentCommand;

public final class CourseLearningService implements CourseLearningUseCase {

    private static final int MAXIMUM_VIDEO_URL_LENGTH = 2048;
    private static final int MAXIMUM_MATERIAL_BYTES = 20 * 1024 * 1024;
    private static final int MAXIMUM_STUDENTS_PER_IMPORT = 1_000;
    private static final Map<String, String> MATERIAL_CONTENT_TYPES = Map.of(
            "pdf", "application/pdf",
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "ppt", "application/vnd.ms-powerpoint",
            "pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    );

    private final CourseRepository courseRepository;
    private final CourseTopicRepository topicRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository progressRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseMaterialRepository materialRepository;
    private final CourseFileStorage fileStorage;
    private final UserRepository userRepository;
    private final Clock clock;

    public CourseLearningService(
            CourseRepository courseRepository,
            CourseTopicRepository topicRepository,
            LessonRepository lessonRepository,
            LessonProgressRepository progressRepository,
            CourseEnrollmentRepository enrollmentRepository,
            CourseMaterialRepository materialRepository,
            CourseFileStorage fileStorage,
            UserRepository userRepository,
            Clock clock
    ) {
        this.courseRepository = courseRepository;
        this.topicRepository = topicRepository;
        this.lessonRepository = lessonRepository;
        this.progressRepository = progressRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.materialRepository = materialRepository;
        this.fileStorage = fileStorage;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Override
    public Course createCourse(CreateCourseCommand command) {
        Objects.requireNonNull(command, "Create course command is required");
        requireTeacherOrAdmin(command.actorRole());
        Objects.requireNonNull(command.actorId(), "Actor id is required");
        var slug = Course.normalizeSlug(command.slug());
        if (courseRepository.existsBySlug(slug)) {
            throw new CourseSlugAlreadyExistsException(slug);
        }

        return courseRepository.save(new Course(
                UUID.randomUUID(),
                slug,
                command.title(),
                command.description(),
                command.actorId(),
                command.startDate() == null ? LocalDate.now(clock) : command.startDate(),
                command.endDate(),
                Instant.now(clock)
        ));
    }

    @Override
    public List<ManagedCourse> listManagedCourses(UUID actorId, UserRole actorRole) {
        requireTeacherOrAdmin(actorRole);
        Objects.requireNonNull(actorId, "Actor id is required");
        var courses = actorRole == UserRole.ADMIN
                ? courseRepository.findAll()
                : courseRepository.findByTeacherId(actorId);
        var today = LocalDate.now(clock);
        return courses.stream()
                .map(course -> new ManagedCourse(
                        course,
                        userRepository.findById(course.teacherId()).map(User::name).orElse("Giảng viên DevEdu"),
                        enrollmentRepository.countByCourseId(course.id()),
                        course.statusOn(today)))
                .toList();
    }

    @Override
    public CourseTopic createTopic(CreateCourseTopicCommand command) {
        Objects.requireNonNull(command, "Create course topic command is required");
        var course = getCourse(command.courseId());
        requireCourseManager(course, command.actorId(), command.actorRole());

        return topicRepository.save(new CourseTopic(
                UUID.randomUUID(),
                course.id(),
                command.title(),
                command.position(),
                Instant.now(clock)
        ));
    }

    @Override
    public Lesson createLesson(CreateLessonCommand command) {
        Objects.requireNonNull(command, "Create lesson command is required");
        var topic = getTopic(command.topicId());
        var course = getCourse(topic.courseId());
        requireCourseManager(course, command.actorId(), command.actorRole());

        return lessonRepository.save(new Lesson(
                UUID.randomUUID(),
                topic.id(),
                command.title(),
                command.content(),
                null,
                command.position(),
                Instant.now(clock)
        ));
    }

    @Override
    public Lesson setLessonVideo(SetLessonVideoCommand command) {
        Objects.requireNonNull(command, "Set lesson video command is required");
        var lesson = getLesson(command.lessonId());
        var topic = getTopic(lesson.topicId());
        var course = getCourse(topic.courseId());
        requireCourseManager(course, command.actorId(), command.actorRole());
        return lessonRepository.save(lesson.withVideoUrl(validateVideoUrl(command.videoUrl())));
    }

    @Override
    public List<Course> listCourses() {
        return courseRepository.findAll();
    }

    @Override
    public CourseDetails getCourseBySlug(String slug) {
        var normalizedSlug = Course.normalizeSlug(slug);
        var course = courseRepository.findBySlug(normalizedSlug)
                .orElseThrow(() -> new CourseResourceNotFoundException("Course", normalizedSlug));
        var topics = topicRepository.findAllByCourseId(course.id());
        var topicIds = topics.stream().map(CourseTopic::id).toList();
        var lessonsByTopic = (topicIds.isEmpty() ? List.<Lesson>of() : lessonRepository.findAllByTopicIds(topicIds))
                .stream()
                .collect(Collectors.groupingBy(Lesson::topicId));
        var topicDetails = topics.stream()
                .map(topic -> new CourseTopicDetails(
                        topic,
                        lessonsByTopic.getOrDefault(topic.id(), List.of())
                ))
                .toList();
        return new CourseDetails(course, topicDetails);
    }

    @Override
    public Lesson getLesson(UUID lessonId) {
        Objects.requireNonNull(lessonId, "Lesson id is required");
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new CourseResourceNotFoundException("Lesson", lessonId));
    }

    @Override
    public LessonProgress completeLesson(CompleteLessonCommand command) {
        Objects.requireNonNull(command, "Complete lesson command is required");
        Objects.requireNonNull(command.studentId(), "Student id is required");
        Objects.requireNonNull(command.lessonId(), "Lesson id is required");
        getLesson(command.lessonId());

        return progressRepository.findByStudentIdAndLessonId(command.studentId(), command.lessonId())
                .orElseGet(() -> progressRepository.save(new LessonProgress(
                        UUID.randomUUID(),
                        command.studentId(),
                        command.lessonId(),
                        Instant.now(clock)
                )));
    }

    @Override
    public List<EnrolledCourseStudent> enrollStudents(EnrollCourseStudentsCommand command) {
        Objects.requireNonNull(command, "Enroll students command is required");
        var course = getCourse(command.courseId());
        requireCourseManager(course, command.actorId(), command.actorRole());
        if (command.studentCodesText() == null || command.studentCodesText().isBlank()) {
            throw new IllegalArgumentException("At least one student code is required");
        }
        var codes = new LinkedHashSet<String>();
        for (var value : command.studentCodesText().split("[\\s,;]+")) {
            if (!value.isBlank()) codes.add(value.trim().toUpperCase(Locale.ROOT));
        }
        if (codes.isEmpty()) throw new IllegalArgumentException("At least one student code is required");
        if (codes.size() > MAXIMUM_STUDENTS_PER_IMPORT) {
            throw new IllegalArgumentException("A student import must not exceed 1000 codes");
        }
        var students = codes.stream().map(code -> userRepository.findByStudentCode(code)
                        .filter(user -> user.role() == UserRole.STUDENT)
                        .orElseThrow(() -> new CourseResourceNotFoundException("Student code", code)))
                .toList();
        var enrolledAt = Instant.now(clock);
        var newEnrollments = students.stream()
                .filter(student -> !enrollmentRepository.existsByCourseIdAndStudentId(course.id(), student.id()))
                .map(student -> new CourseEnrollment(course.id(), student.id(), enrolledAt))
                .toList();
        if (!newEnrollments.isEmpty()) enrollmentRepository.saveAll(newEnrollments);
        return listEnrolledStudents(new ManageCourseCommand(command.actorId(), command.actorRole(), course.id()));
    }

    @Override
    public List<EnrolledCourseStudent> listEnrolledStudents(ManageCourseCommand command) {
        Objects.requireNonNull(command, "Manage course command is required");
        var course = getCourse(command.courseId());
        requireCourseManager(course, command.actorId(), command.actorRole());
        return enrollmentRepository.findAllByCourseId(course.id()).stream()
                .map(enrollment -> new EnrolledCourseStudent(
                        userRepository.findById(enrollment.studentId())
                                .orElseThrow(() -> new CourseResourceNotFoundException("Student", enrollment.studentId())),
                        enrollment.enrolledAt(), enrollment.displayName()))
                .toList();
    }

    @Override
    public List<User> searchAvailableStudents(ManageCourseCommand command, String query) {
        Objects.requireNonNull(command, "Manage course command is required");
        var course = getCourse(command.courseId());
        requireCourseManager(course, command.actorId(), command.actorRole());
        var enrolledIds = new java.util.HashSet<>(enrollmentRepository.findStudentIdsByCourseId(course.id()));
        return userRepository.searchStudents(query, 50).stream()
                .filter(student -> !enrolledIds.contains(student.id()))
                .toList();
    }

    @Override
    public List<EnrolledCourseStudent> addStudents(ManageCourseStudentsCommand command) {
        var course = validateStudentManagement(command);
        var students = resolveStudents(command.studentIds());
        var enrolledAt = Instant.now(clock);
        var additions = students.stream()
                .filter(student -> !enrollmentRepository.existsByCourseIdAndStudentId(course.id(), student.id()))
                .map(student -> new CourseEnrollment(course.id(), student.id(), enrolledAt))
                .toList();
        if (!additions.isEmpty()) enrollmentRepository.saveAll(additions);
        return listEnrolledStudents(new ManageCourseCommand(command.actorId(), command.actorRole(), course.id()));
    }

    @Override
    public List<EnrolledCourseStudent> removeStudents(ManageCourseStudentsCommand command) {
        var course = validateStudentManagement(command);
        var students = resolveStudents(command.studentIds());
        enrollmentRepository.deleteByCourseIdAndStudentIds(course.id(), students.stream().map(User::id).toList());
        return listEnrolledStudents(new ManageCourseCommand(command.actorId(), command.actorRole(), course.id()));
    }

    @Override
    public List<EnrolledCourseStudent> updateStudent(UpdateCourseStudentCommand command) {
        Objects.requireNonNull(command, "Update course student command is required");
        var course = getCourse(command.courseId());
        requireCourseManager(course, command.actorId(), command.actorRole());
        var student = userRepository.findByPublicId(command.studentPublicId())
                .filter(user -> user.role() == UserRole.STUDENT)
                .orElseThrow(() -> new CourseResourceNotFoundException("Student", command.studentPublicId()));
        if (!enrollmentRepository.existsByCourseIdAndStudentId(course.id(), student.id())) {
            throw new CourseResourceNotFoundException("Course student", command.studentPublicId());
        }
        var displayName = command.displayName() == null ? "" : command.displayName().trim();
        if (displayName.isBlank() || displayName.length() > 100) {
            throw new IllegalArgumentException("Class display name must contain 1 to 100 characters");
        }
        enrollmentRepository.updateDisplayName(course.id(), student.id(), displayName);
        return listEnrolledStudents(new ManageCourseCommand(command.actorId(), command.actorRole(), course.id()));
    }

    private Course validateStudentManagement(ManageCourseStudentsCommand command) {
        Objects.requireNonNull(command, "Manage course students command is required");
        var course = getCourse(command.courseId());
        requireCourseManager(course, command.actorId(), command.actorRole());
        if (command.studentIds() == null || command.studentIds().isEmpty()) {
            throw new IllegalArgumentException("At least one student is required");
        }
        if (command.studentIds().size() > MAXIMUM_STUDENTS_PER_IMPORT) {
            throw new IllegalArgumentException("A student operation must not exceed 1000 students");
        }
        return course;
    }

    private List<User> resolveStudents(List<Long> publicIds) {
        var uniqueIds = new LinkedHashSet<>(publicIds);
        if (uniqueIds.size() != publicIds.size() || uniqueIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("Student ids must be unique positive numbers");
        }
        return uniqueIds.stream().map(id -> userRepository.findByPublicId(id)
                        .filter(user -> user.role() == UserRole.STUDENT)
                        .orElseThrow(() -> new CourseResourceNotFoundException("Student", id)))
                .toList();
    }

    @Override
    public CourseMaterial uploadMaterial(UploadCourseMaterialCommand command) {
        Objects.requireNonNull(command, "Upload material command is required");
        var course = getCourse(command.courseId());
        requireCourseManager(course, command.actorId(), command.actorRole());
        if (command.content() == null || command.content().length == 0) {
            throw new IllegalArgumentException("Material file must not be empty");
        }
        if (command.content().length > MAXIMUM_MATERIAL_BYTES) {
            throw new IllegalArgumentException("Material file must not exceed 20 MB");
        }
        var originalFileName = normalizeFileName(command.originalFileName());
        var extension = extensionOf(originalFileName);
        var contentType = MATERIAL_CONTENT_TYPES.get(extension);
        if (contentType == null) {
            throw new IllegalArgumentException("Only PDF, Word and PowerPoint files are allowed");
        }
        var storageKey = UUID.randomUUID() + "." + extension;
        var material = new CourseMaterial(UUID.randomUUID(), course.id(), command.title(), originalFileName,
                storageKey, contentType, command.content().length, Instant.now(clock));
        fileStorage.store(storageKey, command.content());
        try {
            return materialRepository.save(material);
        } catch (RuntimeException exception) {
            fileStorage.delete(storageKey);
            throw exception;
        }
    }

    @Override
    public List<CourseMaterial> listMaterials(AccessCourseMaterialsCommand command) {
        Objects.requireNonNull(command, "Access course materials command is required");
        var course = getCourse(command.courseId());
        requireCourseAccess(course, command.actorId(), command.actorRole());
        return materialRepository.findAllByCourseId(course.id());
    }

    @Override
    public CourseMaterialContent downloadMaterial(DownloadCourseMaterialCommand command) {
        Objects.requireNonNull(command, "Download course material command is required");
        var material = materialRepository.findById(command.materialId())
                .orElseThrow(() -> new CourseResourceNotFoundException("Course material", command.materialId()));
        var course = getCourse(material.courseId());
        requireCourseAccess(course, command.actorId(), command.actorRole());
        return new CourseMaterialContent(material, fileStorage.load(material.storageKey()));
    }

    private Course getCourse(UUID courseId) {
        Objects.requireNonNull(courseId, "Course id is required");
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseResourceNotFoundException("Course", courseId));
    }

    private CourseTopic getTopic(UUID topicId) {
        Objects.requireNonNull(topicId, "Course topic id is required");
        return topicRepository.findById(topicId)
                .orElseThrow(() -> new CourseResourceNotFoundException("Course topic", topicId));
    }

    private void requireCourseManager(Course course, UUID actorId, UserRole actorRole) {
        requireTeacherOrAdmin(actorRole);
        Objects.requireNonNull(actorId, "Actor id is required");
        if (actorRole != UserRole.ADMIN && !course.teacherId().equals(actorId)) {
            throw new CourseManagementForbiddenException();
        }
    }

    private void requireTeacherOrAdmin(UserRole role) {
        if (role != UserRole.TEACHER && role != UserRole.ADMIN) {
            throw new CourseManagementForbiddenException();
        }
    }

    private void requireCourseAccess(Course course, UUID actorId, UserRole actorRole) {
        Objects.requireNonNull(actorId, "Actor id is required");
        if (actorRole == UserRole.ADMIN || (actorRole == UserRole.TEACHER && course.teacherId().equals(actorId))) return;
        if (actorRole == UserRole.STUDENT && enrollmentRepository.existsByCourseIdAndStudentId(course.id(), actorId)) return;
        throw new CourseManagementForbiddenException();
    }

    private String normalizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) throw new IllegalArgumentException("File name is required");
        var normalized = fileName.replace('\\', '/');
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (normalized.isBlank() || normalized.length() > 255) throw new IllegalArgumentException("File name is invalid");
        return normalized;
    }

    private String extensionOf(String fileName) {
        var dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String validateVideoUrl(String videoUrl) {
        if (videoUrl == null || videoUrl.isBlank()) {
            throw new IllegalArgumentException("Video URL is required");
        }
        var normalized = videoUrl.trim();
        if (normalized.length() > MAXIMUM_VIDEO_URL_LENGTH) {
            throw new IllegalArgumentException("Video URL must not exceed 2048 characters");
        }
        try {
            var uri = URI.create(normalized);
            var scheme = uri.getScheme();
            if ((!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
                    || uri.getHost() == null) {
                throw new IllegalArgumentException("Video URL must use HTTP or HTTPS");
            }
            return normalized;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Video URL must use HTTP or HTTPS");
        }
    }
}
