package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.exception.CourseManagementForbiddenException;
import com.devedu.learningplatform.application.exception.CourseResourceNotFoundException;
import com.devedu.learningplatform.application.port.in.CourseClassroomUseCase;
import com.devedu.learningplatform.application.port.in.command.ManageCourseCommand;
import com.devedu.learningplatform.application.port.in.command.ManageCourseProblemCommand;
import com.devedu.learningplatform.application.port.in.result.CourseProblemProgress;
import com.devedu.learningplatform.application.port.in.result.CourseStudentProgress;
import com.devedu.learningplatform.application.port.in.result.StudentCourseDetails;
import com.devedu.learningplatform.application.port.in.result.StudentCourseSummary;
import com.devedu.learningplatform.application.port.out.CourseEnrollmentRepository;
import com.devedu.learningplatform.application.port.out.CourseProblemAssignmentRepository;
import com.devedu.learningplatform.application.port.out.CourseRepository;
import com.devedu.learningplatform.application.port.out.ProblemSubmissionRepository;
import com.devedu.learningplatform.application.port.out.ProgrammingProblemRepository;
import com.devedu.learningplatform.application.port.out.UserRepository;
import com.devedu.learningplatform.domain.model.Course;
import com.devedu.learningplatform.domain.model.CourseProblemAssignment;
import com.devedu.learningplatform.domain.model.User;
import com.devedu.learningplatform.domain.model.UserRole;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class CourseClassroomService implements CourseClassroomUseCase {
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseProblemAssignmentRepository assignmentRepository;
    private final ProgrammingProblemRepository problemRepository;
    private final ProblemSubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public CourseClassroomService(CourseRepository courseRepository, CourseEnrollmentRepository enrollmentRepository,
            CourseProblemAssignmentRepository assignmentRepository, ProgrammingProblemRepository problemRepository,
            ProblemSubmissionRepository submissionRepository, UserRepository userRepository, Clock clock) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.problemRepository = problemRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Override
    public List<StudentCourseSummary> listStudentCourses(UUID actorId, UserRole actorRole) {
        Objects.requireNonNull(actorId, "Actor id is required");
        Objects.requireNonNull(actorRole, "Actor role is required");
        var courseIds = new java.util.LinkedHashSet<>(enrollmentRepository.findCourseIdsByStudentId(actorId));
        if (actorRole == UserRole.TEACHER) {
            courseRepository.findByTeacherId(actorId).stream().map(Course::id).forEach(courseIds::add);
        } else if (actorRole == UserRole.ADMIN) {
            courseRepository.findAll().stream().map(Course::id).forEach(courseIds::add);
        }
        var accepted = submissionRepository.findAcceptedProblemIdsByStudentId(actorId);
        return courseIds.stream()
                .map(this::getCourse).map(course -> summary(course, accepted)).toList();
    }

    @Override
    public StudentCourseDetails getStudentCourse(UUID actorId, UserRole actorRole, UUID courseId) {
        Objects.requireNonNull(actorId, "Actor id is required");
        Objects.requireNonNull(actorRole, "Actor role is required");
        var course = getCourse(courseId);
        var canLearn = enrollmentRepository.existsByCourseIdAndStudentId(course.id(), actorId)
                || actorRole == UserRole.ADMIN
                || (actorRole == UserRole.TEACHER && course.teacherId().equals(actorId));
        if (!canLearn) {
            throw new CourseManagementForbiddenException();
        }
        var accepted = submissionRepository.findAcceptedProblemIdsByStudentId(actorId);
        var problems = assignmentRepository.findAllByCourseId(course.id()).stream()
                .map(item -> new CourseProblemProgress(getProblem(item.problemId()), item.assignedAt(), accepted.contains(item.problemId())))
                .toList();
        return new StudentCourseDetails(summary(course, accepted), problems);
    }

    @Override
    public List<CourseProblemProgress> listManagedProblems(ManageCourseCommand command) {
        var course = requireManager(command.actorId(), command.actorRole(), command.courseId());
        return assignmentRepository.findAllByCourseId(course.id()).stream()
                .map(item -> new CourseProblemProgress(getProblem(item.problemId()), item.assignedAt(), false)).toList();
    }

    @Override
    public List<CourseProblemProgress> assignProblem(ManageCourseProblemCommand command) {
        var course = requireManager(command.actorId(), command.actorRole(), command.courseId());
        getProblem(command.problemId());
        if (!assignmentRepository.existsByCourseIdAndProblemId(course.id(), command.problemId())) {
            assignmentRepository.save(new CourseProblemAssignment(course.id(), command.problemId(), Instant.now(clock)));
        }
        return listManagedProblems(new ManageCourseCommand(command.actorId(), command.actorRole(), course.id()));
    }

    @Override
    public List<CourseProblemProgress> removeProblem(ManageCourseProblemCommand command) {
        var course = requireManager(command.actorId(), command.actorRole(), command.courseId());
        assignmentRepository.deleteByCourseIdAndProblemId(course.id(), command.problemId());
        return listManagedProblems(new ManageCourseCommand(command.actorId(), command.actorRole(), course.id()));
    }

    @Override
    public List<CourseStudentProgress> listStudentProgress(ManageCourseCommand command) {
        var course = requireManager(command.actorId(), command.actorRole(), command.courseId());
        var enrollments = enrollmentRepository.findAllByCourseId(course.id());
        var studentIds = enrollments.stream().map(enrollment -> enrollment.studentId())
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        var usersById = userRepository.findAllByIds(List.copyOf(studentIds)).stream()
                .collect(java.util.stream.Collectors.toMap(User::id, user -> user));
        var acceptedByStudent = submissionRepository.findAcceptedProblemIdsByStudentIds(studentIds);
        var assignedProblemIds = assignmentRepository.findAllByCourseId(course.id()).stream()
                .map(CourseProblemAssignment::problemId).collect(java.util.stream.Collectors.toSet());
        return enrollments.stream().map(enrollment -> {
            var student = java.util.Optional.ofNullable(usersById.get(enrollment.studentId()))
                    .orElseThrow(() -> new CourseResourceNotFoundException("Student", enrollment.studentId()));
            var solved = (int) acceptedByStudent.getOrDefault(student.id(), Set.of()).stream()
                    .filter(assignedProblemIds::contains).count();
            return new CourseStudentProgress(student, enrollment.enrolledAt(), enrollment.displayName(),
                    solved, assignedProblemIds.size());
        }).toList();
    }

    private StudentCourseSummary summary(Course course, Set<UUID> accepted) {
        var assignments = assignmentRepository.findAllByCourseId(course.id());
        var solved = (int) assignments.stream().filter(item -> accepted.contains(item.problemId())).count();
        var teacherName = userRepository.findById(course.teacherId()).map(User::name).orElse("Giảng viên DevEdu");
        return new StudentCourseSummary(course, teacherName, course.statusOn(LocalDate.now(clock)), solved, assignments.size());
    }

    private Course requireManager(UUID actorId, UserRole actorRole, UUID courseId) {
        var course = getCourse(courseId);
        if (actorRole != UserRole.ADMIN && (actorRole != UserRole.TEACHER || !course.teacherId().equals(actorId))) {
            throw new CourseManagementForbiddenException();
        }
        return course;
    }

    private Course getCourse(UUID courseId) {
        return courseRepository.findById(Objects.requireNonNull(courseId, "Course id is required"))
                .orElseThrow(() -> new CourseResourceNotFoundException("Course", courseId));
    }

    private com.devedu.learningplatform.domain.model.ProgrammingProblem getProblem(UUID problemId) {
        return problemRepository.findById(Objects.requireNonNull(problemId, "Problem id is required"))
                .orElseThrow(() -> new CourseResourceNotFoundException("Programming problem", problemId));
    }
}
