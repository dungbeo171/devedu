package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.exception.ProgrammingProblemNotFoundException;
import com.devedu.learningplatform.application.port.in.command.SubmitProblemCommand;
import com.devedu.learningplatform.application.port.in.result.JudgeResult;
import com.devedu.learningplatform.application.port.out.ProblemSubmissionRepository;
import com.devedu.learningplatform.application.port.out.ProgrammingProblemRepository;
import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ProblemSubmission;
import com.devedu.learningplatform.domain.model.ProblemTestCase;
import com.devedu.learningplatform.domain.model.ProblemTopic;
import com.devedu.learningplatform.domain.model.ProgrammingProblem;
import com.devedu.learningplatform.domain.model.SubmissionStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProgrammingProblemsServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-22T10:00:00Z");
    private static final UUID STUDENT_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000001");

    private final TestProblemRepository problemRepository = new TestProblemRepository();
    private final TestSubmissionRepository submissionRepository = new TestSubmissionRepository();
    private final ProgrammingProblemsService service = new ProgrammingProblemsService(
            problemRepository,
            submissionRepository,
            problemId -> List.of(new ProblemTestCase(UUID.randomUUID(), problemId, "input", "output", 1000, 1)),
            command -> new JudgeResult(SubmissionStatus.ACCEPTED, "All test cases passed", 1, 1, 25),
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void listsProblemsByTopic() {
        var problems = service.list(ProblemTopic.ALGORITHMS);

        assertThat(problemRepository.lastTopic).isEqualTo(ProblemTopic.ALGORITHMS);
        assertThat(problems).containsExactly(problemRepository.problem);
    }

    @Test
    void getsAProblemByNormalizedSlug() {
        var problem = service.getBySlug("  BINARY-SEARCH ");

        assertThat(problem.slug()).isEqualTo("binary-search");
    }

    @Test
    void reportsWhenAProblemDoesNotExist() {
        assertThatThrownBy(() -> service.getBySlug("missing"))
                .isInstanceOf(ProgrammingProblemNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void judgesAndStoresASubmission() {
        var submission = service.submit(new SubmitProblemCommand(
                STUDENT_ID,
                "binary-search",
                CodeLanguage.JAVA,
                "class Main {}"
        ));

        assertThat(submission.studentId()).isEqualTo(STUDENT_ID);
        assertThat(submission.problemId()).isEqualTo(problemRepository.problem.id());
        assertThat(submission.language()).isEqualTo(CodeLanguage.JAVA);
        assertThat(submission.status()).isEqualTo(SubmissionStatus.ACCEPTED);
        assertThat(submission.passedTests()).isEqualTo(1);
        assertThat(submission.totalTests()).isEqualTo(1);
        assertThat(submission.executionTimeMillis()).isEqualTo(25);
        assertThat(submission.submittedAt()).isEqualTo(NOW);
        assertThat(submissionRepository.saved).isEqualTo(submission);
    }

    private static final class TestProblemRepository implements ProgrammingProblemRepository {

        private final ProgrammingProblem problem = new ProgrammingProblem(
                UUID.fromString("10000000-0000-0000-0000-000000000007"),
                "binary-search",
                "Tìm kiếm nhị phân",
                "Tìm một giá trị.",
                "Mô tả bài toán.",
                ProblemTopic.ALGORITHMS,
                NOW
        );
        private ProblemTopic lastTopic;

        @Override
        public List<ProgrammingProblem> findAll(ProblemTopic topic) {
            lastTopic = topic;
            return List.of(problem);
        }

        @Override
        public Optional<ProgrammingProblem> findBySlug(String slug) {
            return problem.slug().equals(slug) ? Optional.of(problem) : Optional.empty();
        }
    }

    private static final class TestSubmissionRepository implements ProblemSubmissionRepository {

        private ProblemSubmission saved;

        @Override
        public ProblemSubmission save(ProblemSubmission submission) {
            saved = submission;
            return submission;
        }
    }
}
