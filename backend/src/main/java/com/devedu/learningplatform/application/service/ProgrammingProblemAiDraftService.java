package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.exception.ProblemGenerationException;
import com.devedu.learningplatform.application.port.in.GenerateProgrammingProblemDraftUseCase;
import com.devedu.learningplatform.application.port.in.result.GeneratedProgrammingProblemDraft;
import com.devedu.learningplatform.application.port.out.ProgrammingProblemDraftGenerator;
import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ProblemTopic;

import java.text.Normalizer;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Objects;

public final class ProgrammingProblemAiDraftService implements GenerateProgrammingProblemDraftUseCase {

    private static final int MINIMUM_PROMPT_LENGTH = 20;
    private static final int MAXIMUM_PROMPT_LENGTH = 4_000;
    private final ProgrammingProblemDraftGenerator generator;

    public ProgrammingProblemAiDraftService(ProgrammingProblemDraftGenerator generator) {
        this.generator = generator;
    }

    @Override
    public GeneratedProgrammingProblemDraft generate(String description) {
        var prompt = description == null ? "" : description.trim();
        if (prompt.length() < MINIMUM_PROMPT_LENGTH) {
            throw new IllegalArgumentException("Mô tả cần có ít nhất 20 ký tự");
        }
        if (prompt.length() > MAXIMUM_PROMPT_LENGTH) {
            throw new IllegalArgumentException("Mô tả không được vượt quá 4000 ký tự");
        }

        var generated = Objects.requireNonNull(generator.generate(prompt), "Generated problem draft is required");
        validateText(generated.title(), "Tên bài tập", 180);
        validateText(generated.summary(), "Tóm tắt", 500);
        validateText(generated.description(), "Đề bài", 50_000);
        validateText(generated.inputDescription(), "Yêu cầu input", 50_000);
        validateText(generated.outputDescription(), "Yêu cầu output", 50_000);
        validateOptionalText(generated.sampleInput(), "Input mẫu", 100_000);
        validateOptionalText(generated.sampleOutput(), "Output mẫu", 100_000);
        Objects.requireNonNull(generated.topic(), "Chủ đề do AI tạo không hợp lệ");
        Objects.requireNonNull(generated.difficulty(), "Độ khó do AI tạo không hợp lệ");
        if (generated.allowedLanguages() == null || generated.allowedLanguages().isEmpty()) {
            throw invalidResponse("AI không trả về ngôn ngữ hợp lệ");
        }
        if (generated.topic() == ProblemTopic.SQL
                && (!generated.allowedLanguages().equals(java.util.Set.of(CodeLanguage.MYSQL)))) {
            throw invalidResponse("Bài SQL do AI tạo phải chỉ sử dụng MySQL");
        }
        if (generated.topic() != ProblemTopic.SQL && generated.allowedLanguages().contains(CodeLanguage.MYSQL)) {
            throw invalidResponse("AI trả về ngôn ngữ MySQL cho bài không thuộc chủ đề SQL");
        }

        var filteredStarterCodes = new EnumMap<CodeLanguage, String>(CodeLanguage.class);
        for (var language : generated.allowedLanguages()) {
            var starterCode = generated.starterCodes() == null ? null : generated.starterCodes().get(language);
            validateText(starterCode, "Code có sẵn cho " + language, 100_000);
            filteredStarterCodes.put(language, starterCode.trim());
        }
        if (generated.testCases() == null || generated.testCases().size() < 3 || generated.testCases().size() > 10) {
            throw invalidResponse("AI phải tạo từ 3 đến 10 test case");
        }
        var testCases = generated.testCases().stream().map(testCase -> {
            if (testCase == null) throw invalidResponse("AI trả về test case không hợp lệ");
            validateOptionalText(testCase.input(), "Input test case", 100_000);
            validateOptionalText(testCase.expectedOutput(), "Output test case", 100_000);
            if (testCase.timeLimitMillis() < 100 || testCase.timeLimitMillis() > 30_000) {
                throw invalidResponse("Time limit do AI tạo phải từ 100 đến 30000 ms");
            }
            return new GeneratedProgrammingProblemDraft.TestCase(
                    valueOrEmpty(testCase.input()), valueOrEmpty(testCase.expectedOutput()), testCase.timeLimitMillis()
            );
        }).toList();

        return new GeneratedProgrammingProblemDraft(
                slugify(generated.title()),
                generated.title().trim(),
                generated.summary().trim(),
                generated.description().trim(),
                generated.inputDescription().trim(),
                generated.outputDescription().trim(),
                valueOrEmpty(generated.sampleInput()),
                valueOrEmpty(generated.sampleOutput()),
                generated.topic(),
                generated.difficulty(),
                java.util.Set.copyOf(generated.allowedLanguages()),
                java.util.Map.copyOf(filteredStarterCodes),
                testCases
        );
    }

    private void validateText(String value, String field, int maximumLength) {
        if (value == null || value.isBlank()) throw invalidResponse(field + " do AI tạo đang để trống");
        validateOptionalText(value, field, maximumLength);
    }

    private void validateOptionalText(String value, String field, int maximumLength) {
        if (value != null && value.length() > maximumLength) {
            throw invalidResponse(field + " do AI tạo vượt quá giới hạn");
        }
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String slugify(String value) {
        var normalized = Normalizer.normalize(value.trim().toLowerCase(Locale.forLanguageTag("vi")).replace('đ', 'd'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (normalized.isBlank()) throw invalidResponse("AI không tạo được slug hợp lệ");
        return normalized.substring(0, Math.min(normalized.length(), 120)).replaceAll("-+$", "");
    }

    private ProblemGenerationException invalidResponse(String message) {
        return new ProblemGenerationException(ProblemGenerationException.Kind.INVALID_RESPONSE, message);
    }
}
