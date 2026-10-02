package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.port.in.command.ExecuteCodeCommand;
import com.devedu.learningplatform.application.port.in.result.CodeExecutionResult;
import com.devedu.learningplatform.application.port.out.CodeExecutionPort;
import com.devedu.learningplatform.domain.model.CodeLanguage;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

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

    @Test
    void forwardsJavaFilesAndKeepsLegacyRequestsCompatible() {
        var files = Map.of("Helper.java", "public class Helper {}");
        service.execute(new ExecuteCodeCommand(CodeLanguage.JAVA, "public class Main {}", null, files));
        verify(sandbox).executeCode(new ExecuteCodeCommand(CodeLanguage.JAVA, "public class Main {}", "", files));
        service.execute(new ExecuteCodeCommand(CodeLanguage.JAVA, "public class Main {}", null, null));
        verify(sandbox).executeCode(new ExecuteCodeCommand(CodeLanguage.JAVA, "public class Main {}", "", Map.of()));
    }

    @Test
    void rejectsUnsafeOrReservedFileNamesBeforeSandbox() {
        for (var name : new String[]{"../Escape.java", "/tmp/Escape.java", "dir/File.java", "dir\\File.java",
                "Main.java", "main.java", "-Option.java", "File.txt", "A.java;echo", "CON.java", "LPT1.java"}) {
            assertThatThrownBy(() -> service.execute(new ExecuteCodeCommand(CodeLanguage.JAVA,
                    "public class Main {}", "", Map.of(name, "")))).isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(sandbox);
    }

    @Test
    void rejectsDuplicateCaseInsensitiveNamesAndNullContent() {
        assertThatThrownBy(() -> service.execute(new ExecuteCodeCommand(CodeLanguage.JAVA, "main", "",
                Map.of("Helper.java", "", "helper.java", "")))).isInstanceOf(IllegalArgumentException.class);
        var files = new HashMap<String, String>();
        files.put("Helper.java", null);
        assertThatThrownBy(() -> service.execute(new ExecuteCodeCommand(CodeLanguage.JAVA, "main", "", files)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(sandbox);
    }

    @Test
    void limitsFileCountTotalSizeAndSupportedLanguage() {
        var files = new HashMap<String, String>();
        for (int i = 0; i < 20; i++) files.put("Class" + i + ".java", "");
        assertThatThrownBy(() -> service.execute(new ExecuteCodeCommand(CodeLanguage.JAVA, "main", "", files)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("20 files");
        assertThatThrownBy(() -> service.execute(new ExecuteCodeCommand(CodeLanguage.JAVA, "main", "",
                Map.of("Helper.java", "x".repeat(100_000)))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Total source");
        assertThatThrownBy(() -> service.execute(new ExecuteCodeCommand(CodeLanguage.PYTHON, "print(1)", "",
                Map.of("Helper.java", ""))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("only supported for Java");
        verifyNoInteractions(sandbox);
    }
}
