package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.exception.CourseManagementForbiddenException;
import com.devedu.learningplatform.application.port.in.command.ManageCourseProblemCommand;
import com.devedu.learningplatform.application.port.out.CourseEnrollmentRepository;
import com.devedu.learningplatform.application.port.out.CourseProblemAssignmentRepository;
import com.devedu.learningplatform.application.port.out.CourseRepository;
import com.devedu.learningplatform.application.port.out.ProblemSubmissionRepository;
import com.devedu.learningplatform.application.port.out.ProgrammingProblemRepository;
import com.devedu.learningplatform.application.port.out.UserRepository;
import com.devedu.learningplatform.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseClassroomServiceTest {
    private static final UUID COURSE = UUID.randomUUID();
    private static final UUID TEACHER = UUID.randomUUID();
    private static final UUID STUDENT = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-02T08:00:00Z");
    private final TestCourses courses = new TestCourses();
    private final TestEnrollments enrollments = new TestEnrollments();
    private final TestAssignments assignments = new TestAssignments();
    private final TestProblems problems = new TestProblems();
    private final TestSubmissions submissions = new TestSubmissions();
    private final TestUsers users = new TestUsers();
    private final CourseClassroomService service = new CourseClassroomService(courses, enrollments, assignments,
            problems, submissions, users, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void studentSeesOnlyEnrolledCourseAndProgressFromAcceptedSubmissions() {
        enrollments.enrolled = true;
        for (int index = 1; index <= 5; index++) {
            var problem = problem(index);
            problems.values.put(problem.id(), problem);
            assignments.values.add(new CourseProblemAssignment(COURSE, problem.id(), NOW));
            if (index == 1) submissions.accepted.add(problem.id());
        }

        var details = service.getStudentCourse(STUDENT, COURSE);

        assertThat(details.summary().solvedProblems()).isEqualTo(1);
        assertThat(details.summary().totalProblems()).isEqualTo(5);
        assertThat(details.summary().progressPercent()).isEqualTo(20);
        assertThat(details.problems()).filteredOn(item -> item.solved()).hasSize(1);
    }

    @Test
    void studentCannotOpenCourseWithoutEnrollment() {
        assertThatThrownBy(() -> service.getStudentCourse(STUDENT, COURSE))
                .isInstanceOf(CourseManagementForbiddenException.class);
    }

    @Test
    void teacherAssignsIdempotentlyAndRemovesProblem() {
        var problem = problem(1);
        problems.values.put(problem.id(), problem);
        var command = new ManageCourseProblemCommand(TEACHER, UserRole.TEACHER, COURSE, problem.id());
        service.assignProblem(command);
        service.assignProblem(command);
        assertThat(assignments.values).hasSize(1);
        assertThat(service.removeProblem(command)).isEmpty();
    }

    private ProgrammingProblem problem(int index) {
        return new ProgrammingProblem(UUID.randomUUID(), "bai-" + index, "Bài " + index, "Tóm tắt", "Đề bài",
                "", "", ProblemTopic.JAVA, ProblemDifficulty.EASY, Set.of(CodeLanguage.JAVA),
                Map.of(CodeLanguage.JAVA, "class Main {}"), NOW);
    }

    private static final class TestCourses implements CourseRepository {
        final Course course = new Course(COURSE, "java-2026", "Java nâng cao", "", TEACHER,
                LocalDate.of(2026, 9, 1), null, NOW);
        public boolean existsBySlug(String slug) { return false; }
        public Course save(Course value) { return value; }
        public List<Course> findAll() { return List.of(course); }
        public Optional<Course> findById(UUID id) { return id.equals(COURSE) ? Optional.of(course) : Optional.empty(); }
        public Optional<Course> findBySlug(String slug) { return Optional.of(course); }
    }
    private static final class TestEnrollments implements CourseEnrollmentRepository {
        boolean enrolled;
        public boolean existsByCourseIdAndStudentId(UUID courseId, UUID studentId) { return enrolled; }
        public List<CourseEnrollment> saveAll(List<CourseEnrollment> values) { return values; }
        public List<UUID> findStudentIdsByCourseId(UUID courseId) { return enrolled ? List.of(STUDENT) : List.of(); }
        public List<UUID> findCourseIdsByStudentId(UUID studentId) { return enrolled ? List.of(COURSE) : List.of(); }
    }
    private static final class TestAssignments implements CourseProblemAssignmentRepository {
        final List<CourseProblemAssignment> values = new ArrayList<>();
        public List<CourseProblemAssignment> findAllByCourseId(UUID courseId) { return values.stream().filter(item -> item.courseId().equals(courseId)).toList(); }
        public boolean existsByCourseIdAndProblemId(UUID courseId, UUID problemId) { return values.stream().anyMatch(item -> item.courseId().equals(courseId) && item.problemId().equals(problemId)); }
        public CourseProblemAssignment save(CourseProblemAssignment value) { values.add(value); return value; }
        public void deleteByCourseIdAndProblemId(UUID courseId, UUID problemId) { values.removeIf(item -> item.courseId().equals(courseId) && item.problemId().equals(problemId)); }
    }
    private static final class TestProblems implements ProgrammingProblemRepository {
        final Map<UUID, ProgrammingProblem> values = new HashMap<>();
        public List<ProgrammingProblem> findAll(ProblemTopic topic, ProblemDifficulty difficulty, CodeLanguage language) { return List.copyOf(values.values()); }
        public Optional<ProgrammingProblem> findBySlug(String slug) { return values.values().stream().filter(item -> item.slug().equals(slug)).findFirst(); }
        public Optional<ProgrammingProblem> findById(UUID id) { return Optional.ofNullable(values.get(id)); }
        public boolean existsBySlug(String slug) { return false; }
        public ProgrammingProblem saveWithTestCases(ProgrammingProblem problem, List<ProblemTestCase> tests) { values.put(problem.id(), problem); return problem; }
        public void deleteById(UUID id) { values.remove(id); }
    }
    private static final class TestSubmissions implements ProblemSubmissionRepository {
        final Set<UUID> accepted = new HashSet<>();
        public ProblemSubmission save(ProblemSubmission submission) { return submission; }
        public Set<UUID> findAcceptedProblemIdsByStudentId(UUID studentId) { return Set.copyOf(accepted); }
    }
    private static final class TestUsers implements UserRepository {
        public boolean existsByEmail(String email) { return false; }
        public Optional<User> findByEmail(String email) { return Optional.empty(); }
        public Optional<User> findById(UUID id) { return id.equals(TEACHER) ? Optional.of(new User(TEACHER, 1, null, "GV000001", "Nguyễn Văn A", "teacher@example.com", "hash", UserRole.TEACHER, NOW)) : Optional.empty(); }
        public Optional<User> findByPublicId(long id) { return Optional.empty(); }
        public Optional<User> findByStudentCode(String code) { return Optional.empty(); }
        public List<User> findAll() { return List.of(); }
        public User save(User user) { return user; }
        public void deleteById(UUID id) {}
    }
}
