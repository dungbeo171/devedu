package com.devedu.learningplatform.application.codeassist;

import com.devedu.learningplatform.domain.codeassist.CodeSuggestionLanguage;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodeSuggestionServiceTest {

    @Test
    void returnsOnlyTheInsertionForTheCurrentLine() {
        var service = new CodeSuggestionService((language, prefix, suffix, context) -> "System.out.println(answer);\nignored");
        var source = "class Main {\n    System.out";

        var result = service.suggest(CodeSuggestionLanguage.JAVA, source, source.length(), "In kết quả");

        assertThat(result.text()).isEqualTo(".println(answer);");
    }

    @Test
    void boundsContextSentToTheGenerator() {
        var capturedPrefix = new AtomicReference<String>();
        var capturedSuffix = new AtomicReference<String>();
        var capturedContext = new AtomicReference<String>();
        var service = new CodeSuggestionService((language, prefix, suffix, context) -> {
            capturedPrefix.set(prefix);
            capturedSuffix.set(suffix);
            capturedContext.set(context);
            return "next";
        });
        var source = "a".repeat(20_000) + "CURSOR" + "b".repeat(4_000);
        var cursor = 20_006;

        service.suggest(CodeSuggestionLanguage.PYTHON, source, cursor, "  Tìm tổng dãy số  ");

        assertThat(capturedPrefix.get()).hasSize(CodeSuggestionService.MAX_PREFIX_CONTEXT).endsWith("CURSOR");
        assertThat(capturedSuffix.get()).hasSize(CodeSuggestionService.MAX_SUFFIX_CONTEXT);
        assertThat(capturedContext.get()).isEqualTo("Tìm tổng dãy số");
    }

    @Test
    void blankSourceDoesNotCallExternalGenerator() {
        var called = new AtomicBoolean();
        var service = new CodeSuggestionService((language, prefix, suffix, context) -> {
            called.set(true);
            return "unexpected";
        });

        assertThat(service.suggest(CodeSuggestionLanguage.CPP, "   ", 2, null).text()).isEmpty();
        assertThat(called).isFalse();
    }

    @Test
    void rejectsInvalidCursorAndOversizedSource() {
        var service = new CodeSuggestionService((language, prefix, suffix, context) -> "next");

        assertThatThrownBy(() -> service.suggest(CodeSuggestionLanguage.CSS, "body {}", 99, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("con trỏ");
        assertThatThrownBy(() -> service.suggest(
                CodeSuggestionLanguage.JAVASCRIPT,
                "x".repeat(CodeSuggestionService.MAX_SOURCE_LENGTH + 1),
                0,
                ""
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("100000");
    }
}
