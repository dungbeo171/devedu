package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.port.in.command.JudgeSubmissionCommand;
import com.devedu.learningplatform.application.port.in.result.JudgeResult;
import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ProblemTestCase;
import com.devedu.learningplatform.domain.model.SubmissionStatus;
import org.junit.jupiter.api.Test;
import java.util.List; import java.util.UUID;
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
}
