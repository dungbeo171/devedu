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
import com.devedu.learningplatform.domain.model.Course;
import com.devedu.learningplatform.domain.model.CourseTopic;
import com.devedu.learningplatform.domain.model.Lesson;
import com.devedu.learningplatform.domain.model.LessonProgress;
import com.devedu.learningplatform.domain.model.UserRole;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public final class CourseLearningService implements CourseLearningUseCase {

    private static final int MAXIMUM_VIDEO_URL_LENGTH = 2048;

    private final CourseRepository courseRepository;
    private final CourseTopicRepository topicRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository progressRepository;
    private final Clock clock;

    public CourseLearningService(
            CourseRepository courseRepository,
            CourseTopicRepository topicRepository,
            LessonRepository lessonRepository,
            LessonProgressRepository progressRepository,
            Clock clock
    ) {
        this.courseRepository = courseRepository;
        this.topicRepository = topicRepository;
        this.lessonRepository = lessonRepository;
        this.progressRepository = progressRepository;
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
                Instant.now(clock)
        ));
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
