package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.exception.ProgrammingProblemNotFoundException;
import com.devedu.learningplatform.application.port.in.command.SubmitProblemCommand;
import com.devedu.learningplatform.application.port.in.command.SaveProblemDraftCommand;
import com.devedu.learningplatform.application.port.in.command.CreateProblemTestCaseCommand;
import com.devedu.learningplatform.application.port.in.command.CreateProgrammingProblemCommand;
import com.devedu.learningplatform.application.port.in.command.UpdateProgrammingProblemCommand;
import com.devedu.learningplatform.application.port.in.command.RunProblemTestsCommand;
import com.devedu.learningplatform.application.port.in.command.JudgeSubmissionCommand;
import com.devedu.learningplatform.application.port.in.result.JudgeResult;
import com.devedu.learningplatform.application.port.out.ProblemSubmissionRepository;
import com.devedu.learningplatform.application.port.out.ProblemDraftRepository;
import com.devedu.learningplatform.application.port.out.ProgrammingProblemRepository;
import com.devedu.learningplatform.application.port.out.ProblemRunStatisticsRepository;
import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ProblemSubmission;
import com.devedu.learningplatform.domain.model.ProblemDraft;
import com.devedu.learningplatform.domain.model.ProblemTestCase;
import com.devedu.learningplatform.domain.model.ProblemTopic;
import com.devedu.learningplatform.domain.model.ProblemDifficulty;
import com.devedu.learningplatform.domain.model.ProgrammingProblem;
import com.devedu.learningplatform.domain.model.ProblemRunStatistics;
import com.devedu.learningplatform.domain.model.SubmissionStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProgrammingProblemsServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-22T10:00:00Z");
    private static final UUID STUDENT_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000001");

    private final TestProblemRepository problemRepository = new TestProblemRepository();
    private final TestSubmissionRepository submissionRepository = new TestSubmissionRepository();
    private final TestDraftRepository draftRepository = new TestDraftRepository();
    private final TestRunStatisticsRepository runStatisticsRepository = new TestRunStatisticsRepository();
    private final ProgrammingProblemsService service = new ProgrammingProblemsService(
            problemRepository,
            submissionRepository,
            draftRepository,
            problemId -> List.of(new ProblemTestCase(UUID.randomUUID(), problemId, "input", "output", 1000, 1)),
            runStatisticsRepository,
            command -> new JudgeResult(SubmissionStatus.ACCEPTED, "All test cases passed", 1, 1, 25, List.of()),
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void listsProblemsByTopic() {
        var problems = service.list(ProblemTopic.ALGORITHMS, ProblemDifficulty.MEDIUM, CodeLanguage.JAVA);

        assertThat(problemRepository.lastTopic).isEqualTo(ProblemTopic.ALGORITHMS);
        assertThat(problemRepository.lastDifficulty).isEqualTo(ProblemDifficulty.MEDIUM);
        assertThat(problemRepository.lastLanguage).isEqualTo(CodeLanguage.JAVA);
        assertThat(problems).singleElement().satisfies(item -> {
            assertThat(item.problem()).isEqualTo(problemRepository.problem);
            assertThat(item.acceptanceRate()).isZero();
        });
    }

    @Test
    void recordsSuccessfulTestRunsAndReturnsTheirAcceptanceRate() {
        service.runTests(new RunProblemTestsCommand("binary-search", CodeLanguage.JAVA, "class Main {}"));

        assertThat(runStatisticsRepository.statistics.totalRuns()).isEqualTo(1);
        assertThat(runStatisticsRepository.statistics.successfulRuns()).isEqualTo(1);
        assertThat(service.list(null, null, null)).singleElement()
                .satisfies(item -> assertThat(item.acceptanceRate()).isEqualTo(100.0));
    }

    @Test
    void usesPublicSampleAsFirstRunCaseAndKeepsFollowingCasesHidden() {
        var capturedCommand = new AtomicReference<JudgeSubmissionCommand>();
        var serviceWithCapturedJudge = new ProgrammingProblemsService(
                problemRepository,
                submissionRepository,
                draftRepository,
                problemId -> List.of(
                        new ProblemTestCase(UUID.randomUUID(), problemId, "hidden-input-1", "hidden-output-1", 1000, 1),
                        new ProblemTestCase(UUID.randomUUID(), problemId, "hidden-input-2", "hidden-output-2", 1000, 2),
                        new ProblemTestCase(UUID.randomUUID(), problemId, "hidden-input-3", "hidden-output-3", 1000, 3)
                ),
                runStatisticsRepository,
                command -> {
                    capturedCommand.set(command);
                    return new JudgeResult(SubmissionStatus.ACCEPTED, "All test cases passed", 3, 3, 25, List.of());
                },
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        serviceWithCapturedJudge.runTests(
                new RunProblemTestsCommand("binary-search", CodeLanguage.JAVA, "class Main {}")
        );

        assertThat(capturedCommand.get().testCases()).hasSize(3);
        assertThat(capturedCommand.get().testCases().get(0))
                .extracting(ProblemTestCase::input, ProblemTestCase::expectedOutput)
                .containsExactly(problemRepository.problem.sampleInput(), problemRepository.problem.sampleOutput());
        assertThat(capturedCommand.get().testCases().get(1))
                .extracting(ProblemTestCase::input, ProblemTestCase::expectedOutput)
                .containsExactly("hidden-input-2", "hidden-output-2");
    }

    @Test
    void getsAProblemByNormalizedSlug() {
        var problem = service.getBySlug("  BINARY-SEARCH ");

        assertThat(problem.slug()).isEqualTo("binary-search");
        assertThat(problem.sampleInput()).isEqualTo("3\n1 2 3\n2\n");
        assertThat(problem.sampleOutput()).isEqualTo("1\n");
    }

    @Test
    void createsAProblemWithHiddenTestCases() {
        var created = service.create(new CreateProgrammingProblemCommand(
                "tong-ba-so",
                "Tổng ba số",
                "Tính tổng ba số nguyên.",
                "Đọc ba số nguyên và in tổng.",
                "Một dòng chứa ba số nguyên a, b và c.",
                "In ra tổng a + b + c trên một dòng.",
                "1 2 3",
                "6",
                ProblemTopic.INTRODUCTION,
                ProblemDifficulty.EASY,
                Set.of(CodeLanguage.CPP, CodeLanguage.JAVA, CodeLanguage.PYTHON),
                Map.of(
                        CodeLanguage.CPP, "int main() { return 0; }",
                        CodeLanguage.JAVA, "public class Main { public static void main(String[] args) {} }",
                        CodeLanguage.PYTHON, "def solve():\n    pass\n"
                ),
                List.of(
                        new CreateProblemTestCaseCommand("1 2 3", "6", 1000),
                        new CreateProblemTestCaseCommand("-5 2 8", "5", 1000),
                        new CreateProblemTestCaseCommand("0 0 0", "0", 1000)
                )
        ));

        assertThat(created.slug()).isEqualTo("tong-ba-so");
        assertThat(problemRepository.saved).isEqualTo(created);
        assertThat(problemRepository.savedTestCases).hasSize(3);
        assertThat(problemRepository.savedTestCases.get(0).problemId()).isEqualTo(created.id());
        assertThat(problemRepository.savedTestCases.get(0).expectedOutput()).isEqualTo("6");
    }

    @Test
    void movesSqlSampleSetupIntoStarterCodeAndRemovesPublicInput() {
        var setupSql = "CREATE TABLE students (id INT);\nINSERT INTO students VALUES (1);";

        var created = service.create(new CreateProgrammingProblemCommand(
                "dem-sinh-vien-sql-moi",
                "Đếm sinh viên SQL mới",
                "Đếm số sinh viên.",
                "Viết truy vấn đếm số sinh viên.",
                "Dữ liệu bảng students.",
                "Trả về số sinh viên.",
                setupSql,
                "1",
                ProblemTopic.SQL,
                ProblemDifficulty.EASY,
                Set.of(CodeLanguage.MYSQL),
                Map.of(CodeLanguage.MYSQL, "SELECT COUNT(*) FROM students;\n"),
                List.of(
                        new CreateProblemTestCaseCommand("old setup", "1", 1000),
                        new CreateProblemTestCaseCommand("hidden setup 2", "2", 1000),
                        new CreateProblemTestCaseCommand("hidden setup 3", "3", 1000)
                )
        ));

        assertThat(created.sampleInput()).isEmpty();
        assertThat(created.inputDescription()).contains("Không có dữ liệu đầu vào từ stdin");
        assertThat(created.starterCodes().get(CodeLanguage.MYSQL))
                .contains("-- DEVEDU_SAMPLE_DATA_BEGIN", setupSql, "-- DEVEDU_SAMPLE_DATA_END")
                .endsWith("SELECT COUNT(*) FROM students;\n");
        assertThat(problemRepository.savedTestCases.get(0).input()).isEqualTo(setupSql);
        assertThat(problemRepository.savedTestCases.get(1).input()).isEqualTo("hidden setup 2");
    }

    @Test
    void rejectsAProblemWithFewerThanThreeTestCases() {
        var command = new CreateProgrammingProblemCommand(
                "tong-hai-so-moi",
                "Tổng hai số mới",
                "Tính tổng hai số nguyên.",
                "Tính tổng hai số nguyên.",
                "Một dòng chứa hai số nguyên a và b.",
                "In ra tổng a + b trên một dòng.",
                "1 2",
                "3",
                ProblemTopic.INTRODUCTION,
                ProblemDifficulty.EASY,
                Set.of(CodeLanguage.CPP),
                Map.of(CodeLanguage.CPP, "int main() { return 0; }"),
                List.of(
                        new CreateProblemTestCaseCommand("1 2", "3", 1000),
                        new CreateProblemTestCaseCommand("-1 1", "0", 1000)
                )
        );

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least three test cases are required");
        assertThat(problemRepository.saved).isNull();
    }

    @Test
    void updatesAProblemAndReplacesItsHiddenTestCases() {
        var updated = service.update(new UpdateProgrammingProblemCommand(
                "binary-search", "tim-kiem-nhi-phan", "Tìm kiếm nhị phân mới",
                "Tóm tắt mới", "Đề bài mới", "Một dòng chứa dữ liệu tìm kiếm.",
                "In ra vị trí tìm được.", "5", "2", ProblemTopic.ALGORITHMS,
                ProblemDifficulty.HARD, Set.of(CodeLanguage.CPP),
                Map.of(CodeLanguage.CPP, "int main() { return 0; }"),
                List.of(
                        new CreateProblemTestCaseCommand("5", "2", 1500),
                        new CreateProblemTestCaseCommand("7", "3", 1500),
                        new CreateProblemTestCaseCommand("9", "4", 1500)
                )
        ));

        assertThat(updated.id()).isEqualTo(problemRepository.problem.id());
        assertThat(updated.slug()).isEqualTo("tim-kiem-nhi-phan");
        assertThat(updated.createdAt()).isEqualTo(NOW);
        assertThat(problemRepository.savedTestCases).hasSize(3);
        assertThat(problemRepository.savedTestCases.get(0))
                .satisfies(testCase -> {
                    assertThat(testCase.problemId()).isEqualTo(updated.id());
                    assertThat(testCase.timeLimitMillis()).isEqualTo(1500);
                });
    }

    @Test
    void deletesAnExistingProblem() {
        service.delete("binary-search");

        assertThat(problemRepository.deletedProblemId).isEqualTo(problemRepository.problem.id());
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
                "Dòng đầu chứa số phần tử và các phần tử của mảng.",
                "In ra vị trí của giá trị cần tìm.",
                "3\n1 2 3\n2\n",
                "1\n",
                ProblemTopic.ALGORITHMS,
                ProblemDifficulty.MEDIUM,
                Set.of(CodeLanguage.CPP, CodeLanguage.JAVA, CodeLanguage.PYTHON),
                Map.of(
                        CodeLanguage.CPP, "int main() { return 0; }",
                        CodeLanguage.JAVA, "public class Main { public static void main(String[] args) {} }",
                        CodeLanguage.PYTHON, "def solve():\n    pass\n"
                ),
                NOW
        );
        private ProblemTopic lastTopic;
        private ProblemDifficulty lastDifficulty;
        private CodeLanguage lastLanguage;
        private ProgrammingProblem saved;
        private List<ProblemTestCase> savedTestCases = List.of();
        private UUID deletedProblemId;

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

        @Override
        public boolean existsBySlug(String slug) {
            return problem.slug().equals(slug);
        }

        @Override
        public ProgrammingProblem saveWithTestCases(ProgrammingProblem problem, List<ProblemTestCase> testCases) {
            saved = problem;
            savedTestCases = testCases;
            return problem;
        }

        @Override
        public void deleteById(UUID problemId) {
            deletedProblemId = problemId;
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

    private static final class TestRunStatisticsRepository implements ProblemRunStatisticsRepository {

        private ProblemRunStatistics statistics;

        @Override
        public void record(UUID problemId, boolean successful) {
            var totalRuns = statistics == null ? 1 : statistics.totalRuns() + 1;
            var successfulRuns = (statistics == null ? 0 : statistics.successfulRuns()) + (successful ? 1 : 0);
            statistics = new ProblemRunStatistics(problemId, totalRuns, successfulRuns);
        }

        @Override
        public Map<UUID, ProblemRunStatistics> findAllByProblemIds(Set<UUID> problemIds) {
            if (statistics == null || !problemIds.contains(statistics.problemId())) return Map.of();
            return Map.of(statistics.problemId(), statistics);
        }
    }
}
