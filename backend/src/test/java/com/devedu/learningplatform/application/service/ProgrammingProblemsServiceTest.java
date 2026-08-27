package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.exception.ProgrammingProblemNotFoundException;
import com.devedu.learningplatform.application.port.in.command.SubmitProblemCommand;
import com.devedu.learningplatform.application.port.in.command.SaveProblemDraftCommand;
import com.devedu.learningplatform.application.port.in.result.JudgeResult;
import com.devedu.learningplatform.application.port.out.ProblemSubmissionRepository;
import com.devedu.learningplatform.application.port.out.ProblemDraftRepository;
import com.devedu.learningplatform.application.port.out.ProgrammingProblemRepository;
import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ProblemSubmission;
import com.devedu.learningplatform.domain.model.ProblemDraft;
import com.devedu.learningplatform.domain.model.ProblemTestCase;
import com.devedu.learningplatform.domain.model.ProblemTopic;
import com.devedu.learningplatform.domain.model.ProblemDifficulty;
import com.devedu.learningplatform.domain.model.ProgrammingProblem;
import com.devedu.learningplatform.domain.model.SubmissionStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProgrammingProblemsServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-22T10:00:00Z");
    private static final UUID STUDENT_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000001");

    private final TestProblemRepository problemRepository = new TestProblemRepository();
    private final TestSubmissionRepository submissionRepository = new TestSubmissionRepository();
    private final TestDraftRepository draftRepository = new TestDraftRepository();
    private final ProgrammingProblemsService service = new ProgrammingProblemsService(
            problemRepository,
            submissionRepository,
            draftRepository,
            problemId -> List.of(new ProblemTestCase(UUID.randomUUID(), problemId, "input", "output", 1000, 1)),
            command -> new JudgeResult(SubmissionStatus.ACCEPTED, "All test cases passed", 1, 1, 25),
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void listsProblemsByTopic() {
        var problems = service.list(ProblemTopic.ALGORITHMS, ProblemDifficulty.MEDIUM, CodeLanguage.JAVA);

        assertThat(problemRepository.lastTopic).isEqualTo(ProblemTopic.ALGORITHMS);
        assertThat(problemRepository.lastDifficulty).isEqualTo(ProblemDifficulty.MEDIUM);
        assertThat(problemRepository.lastLanguage).isEqualTo(CodeLanguage.JAVA);
        assertThat(problems).containsExactly(problemRepository.problem);
    }

    @Test
    void getsAProblemByNormalizedSlug() {
        var problem = service.getBySlug("  BINARY-SEARCH ");

        assertThat(problem.slug()).isEqualTo("binary-search");
        assertThat(problem.sampleInput()).isEqualTo("3\n1 2 3\n2\n");
        assertThat(problem.sampleOutput()).isEqualTo("1\n");
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
        assertThat(draftRepository.saved.sourceCode()).isEqualTo("class Main {}");
        assertThat(draftRepository.saved.language()).isEqualTo(CodeLanguage.JAVA);
    }

    @Test
    void rejectsALanguageThatTheProblemDoesNotAllow() {
        assertThatThrownBy(() -> service.submit(new SubmitProblemCommand(
                STUDENT_ID,
                "binary-search",
                CodeLanguage.HTML,
                "<p>not an algorithm solution</p>"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTML")
                .hasMessageContaining("not allowed");
    }

    @Test
    void listsPersistedAcceptedProblemsForAStudent() {
        submissionRepository.acceptedProblemIds = Set.of(problemRepository.problem.id());

        assertThat(service.listSolvedProblemIds(STUDENT_ID))
                .containsExactly(problemRepository.problem.id());
        assertThat(submissionRepository.requestedStudentId).isEqualTo(STUDENT_ID);
    }

    @Test
    void savesAndLoadsAStudentDraftForAProblem() {
        var saved = service.saveDraft(new SaveProblemDraftCommand(
                STUDENT_ID,
                "binary-search",
                CodeLanguage.PYTHON,
                "print('draft')",
                "sample input"
        ));

        assertThat(saved.problemId()).isEqualTo(problemRepository.problem.id());
        assertThat(saved.studentId()).isEqualTo(STUDENT_ID);
        assertThat(saved.updatedAt()).isEqualTo(NOW);
        assertThat(service.getDraft(STUDENT_ID, "binary-search")).contains(saved);
    }

    private static final class TestProblemRepository implements ProgrammingProblemRepository {

        private final ProgrammingProblem problem = new ProgrammingProblem(
                UUID.fromString("10000000-0000-0000-0000-000000000007"),
                "binary-search",
                "Tìm kiếm nhị phân",
                "Tìm một giá trị.",
                "Mô tả bài toán.",
                "3\n1 2 3\n2\n",
                "1\n",
                ProblemTopic.ALGORITHMS,
                ProblemDifficulty.MEDIUM,
                Set.of(CodeLanguage.CPP, CodeLanguage.JAVA, CodeLanguage.PYTHON),
                NOW
        );
        private ProblemTopic lastTopic;
        private ProblemDifficulty lastDifficulty;
        private CodeLanguage lastLanguage;

        @Override
        public List<ProgrammingProblem> findAll(ProblemTopic topic, ProblemDifficulty difficulty, CodeLanguage language) {
            lastTopic = topic;
            lastDifficulty = difficulty;
            lastLanguage = language;
            return List.of(problem);
        }

        @Override
        public Optional<ProgrammingProblem> findBySlug(String slug) {
            return problem.slug().equals(slug) ? Optional.of(problem) : Optional.empty();
        }
    }

    private static final class TestSubmissionRepository implements ProblemSubmissionRepository {

        private ProblemSubmission saved;
        private Set<UUID> acceptedProblemIds = Set.of();
        private UUID requestedStudentId;

        @Override
        public ProblemSubmission save(ProblemSubmission submission) {
            saved = submission;
            return submission;
        }

        @Override
        public Set<UUID> findAcceptedProblemIdsByStudentId(UUID studentId) {
            requestedStudentId = studentId;
            return acceptedProblemIds;
        }
    }

    private static final class TestDraftRepository implements ProblemDraftRepository {

        private ProblemDraft saved;

        @Override
        public Optional<ProblemDraft> findByStudentIdAndProblemId(UUID studentId, UUID problemId) {
            if (saved == null || !saved.studentId().equals(studentId) || !saved.problemId().equals(problemId)) {
                return Optional.empty();
            }
            return Optional.of(saved);
        }

        @Override
        public ProblemDraft save(ProblemDraft draft) {
            saved = draft;
            return draft;
        }
    }
}
