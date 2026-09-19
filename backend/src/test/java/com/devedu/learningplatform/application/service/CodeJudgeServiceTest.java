package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.port.in.command.JudgeSubmissionCommand;
import com.devedu.learningplatform.application.port.in.result.JudgeResult;
import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ProblemTestCase;
import com.devedu.learningplatform.domain.model.SubmissionStatus;
import org.junit.jupiter.api.Test;
import java.util.List; import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodeJudgeServiceTest {
    @Test void delegatesExecutionToSandboxPort() {
        var expected = new JudgeResult(SubmissionStatus.ACCEPTED, "ok", 1, 1, 20, List.of());
        var service = new CodeJudgeService(command -> expected);
        var testCase = new ProblemTestCase(UUID.randomUUID(), UUID.randomUUID(), "1", "1", 1000, 1);
        assertThat(service.judge(new JudgeSubmissionCommand(UUID.randomUUID(), CodeLanguage.PYTHON, "print(1)", List.of(testCase)))).isEqualTo(expected);
    }

    @Test void rejectsSubmissionWithoutTestCases() {
        var service = new CodeJudgeService(command -> { throw new AssertionError("sandbox must not run"); });
        assertThatThrownBy(() -> service.judge(new JudgeSubmissionCommand(UUID.randomUUID(), CodeLanguage.JAVA,
                "class Main {}", List.of()))).isInstanceOf(IllegalStateException.class).hasMessageContaining("no test cases");
    }

    @Test void removesPublicMySqlSetupBeforeSendingCodeToHiddenTests() {
        var captured = new AtomicReference<JudgeSubmissionCommand>();
        var expected = new JudgeResult(SubmissionStatus.ACCEPTED, "ok", 1, 1, 20, List.of());
        var service = new CodeJudgeService(command -> {
            captured.set(command);
            return expected;
        });
        var testCase = new ProblemTestCase(UUID.randomUUID(), UUID.randomUUID(),
                "CREATE TABLE values_table (value INT);", "1", 1000, 1);
        var sourceCode = "-- DEVEDU_SAMPLE_DATA_BEGIN\n"
                + "CREATE TABLE values_table (value INT);\n"
                + "INSERT INTO values_table VALUES (1);\n"
                + "-- DEVEDU_SAMPLE_DATA_END\n\n"
                + "SELECT COUNT(*) FROM values_table;\n";

        assertThat(service.judge(new JudgeSubmissionCommand(
                UUID.randomUUID(), CodeLanguage.MYSQL, sourceCode, List.of(testCase)
        ))).isEqualTo(expected);

        assertThat(captured.get().sourceCode())
                .isEqualTo("SELECT COUNT(*) FROM values_table;\n")
                .doesNotContain("CREATE TABLE", "DEVEDU_SAMPLE_DATA");
    }
}
