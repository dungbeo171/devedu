package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.exception.ProblemGenerationException;
import com.devedu.learningplatform.application.port.in.result.GeneratedProgrammingProblemDraft;
import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ProblemDifficulty;
import com.devedu.learningplatform.domain.model.ProblemTopic;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProgrammingProblemAiDraftServiceTest {

    @Test
    void validatesAndNormalizesGeneratedDraft() {
        var service = new ProgrammingProblemAiDraftService(prompt -> validDraft());

        var result = service.generate("Tạo bài tìm số lớn nhất trong một dãy số nguyên");

        assertThat(result.slug()).isEqualTo("tim-so-lon-nhat");
        assertThat(result.allowedLanguages()).containsExactly(CodeLanguage.CPP);
        assertThat(result.testCases()).hasSize(3);
    }

    @Test
    void rejectsPromptThatIsTooShortBeforeCallingGemini() {
        var service = new ProgrammingProblemAiDraftService(prompt -> validDraft());

        assertThatThrownBy(() -> service.generate("Bài dễ"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("20 ký tự");
    }

    @Test
    void rejectsGeneratedDraftWithFewerThanThreeTestCases() {
        var invalid = copyWith(
                ProblemTopic.ALGORITHMS,
                Set.of(CodeLanguage.CPP),
                List.of(new GeneratedProgrammingProblemDraft.TestCase("1", "1", 1000))
        );
        var service = new ProgrammingProblemAiDraftService(prompt -> invalid);

        assertThatThrownBy(() -> service.generate("Tạo bài tìm số lớn nhất trong một dãy số nguyên"))
                .isInstanceOf(ProblemGenerationException.class)
                .hasMessageContaining("3 đến 10 test case");
    }

    @Test
    void rejectsMysqlForNonSqlProblem() {
        var invalid = copyWith(
                ProblemTopic.ALGORITHMS,
                Set.of(CodeLanguage.MYSQL),
                validDraft().testCases()
        );
        var service = new ProgrammingProblemAiDraftService(prompt -> invalid);

        assertThatThrownBy(() -> service.generate("Tạo bài tìm số lớn nhất trong một dãy số nguyên"))
                .isInstanceOf(ProblemGenerationException.class)
                .hasMessageContaining("MySQL");
    }

    private GeneratedProgrammingProblemDraft validDraft() {
        return copyWith(
                ProblemTopic.ALGORITHMS,
                Set.of(CodeLanguage.CPP),
                List.of(
                        new GeneratedProgrammingProblemDraft.TestCase("3\n1 2 3", "3", 1000),
                        new GeneratedProgrammingProblemDraft.TestCase("2\n-1 -2", "-1", 1000),
                        new GeneratedProgrammingProblemDraft.TestCase("1\n7", "7", 1000)
                )
        );
    }

    private GeneratedProgrammingProblemDraft copyWith(
            ProblemTopic topic,
            Set<CodeLanguage> languages,
            List<GeneratedProgrammingProblemDraft.TestCase> testCases
    ) {
        return new GeneratedProgrammingProblemDraft(
                "ignored-by-service",
                "Tìm số lớn nhất",
                "Tìm giá trị lớn nhất trong dãy.",
                "Cho một dãy số nguyên, hãy in ra phần tử lớn nhất.",
                "Dòng đầu là n, dòng sau gồm n số nguyên.",
                "In ra giá trị lớn nhất.",
                "3\n1 2 3",
                "3",
                topic,
                ProblemDifficulty.EASY,
                languages,
                languages.contains(CodeLanguage.MYSQL)
                        ? Map.of(CodeLanguage.MYSQL, "SELECT 1;")
                        : Map.of(CodeLanguage.CPP, "#include <iostream>\nint main() {}"),
                testCases
        );
    }
}
