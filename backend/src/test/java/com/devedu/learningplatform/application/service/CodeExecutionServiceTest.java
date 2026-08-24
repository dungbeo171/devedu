package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.port.in.command.ExecuteCodeCommand;
import com.devedu.learningplatform.application.port.in.result.CodeExecutionResult;
import com.devedu.learningplatform.application.port.out.CodeExecutionPort;
import com.devedu.learningplatform.domain.model.CodeLanguage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CodeExecutionServiceTest {

    private final CodeExecutionPort sandbox = mock(CodeExecutionPort.class);
    private final CodeExecutionService service = new CodeExecutionService(sandbox);

    @Test
    void executesCodeThroughSandbox() {
        when(sandbox.executeCode(any())).thenReturn(new CodeExecutionResult(
                CodeLanguage.PYTHON,
                CodeExecutionResult.Status.SUCCESS,
                "Hello\n"
        ));

        var result = service.execute(new ExecuteCodeCommand(
                CodeLanguage.PYTHON,
                "print('Hello')",
                null
        ));

        assertThat(result.status()).isEqualTo(CodeExecutionResult.Status.SUCCESS);
        assertThat(result.output()).isEqualTo("Hello\n");
        verify(sandbox).executeCode(new ExecuteCodeCommand(
                CodeLanguage.PYTHON,
                "print('Hello')",
                ""
        ));
    }

    @Test
    void rejectsBlankCode() {
        assertThatThrownBy(() ->
                service.execute(new ExecuteCodeCommand(CodeLanguage.JAVA, "  ", "")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Code is required");
    }

    @Test
    void rejectsOversizedInput() {
        assertThatThrownBy(() -> service.execute(new ExecuteCodeCommand(
                CodeLanguage.CPP,
                "int main() {}",
                "x".repeat(100_001)
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Input must not exceed 100000 characters");
    }
}
