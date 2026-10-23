package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.application.port.in.result.GeneratedProgrammingProblemDraft;
import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ProblemDifficulty;
import com.devedu.learningplatform.domain.model.ProblemTopic;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record GeneratedProgrammingProblemDraftResponse(
        String slug,
        String title,
        String summary,
        String description,
        String inputDescription,
        String outputDescription,
        String sampleInput,
        String sampleOutput,
        ProblemTopic topic,
        ProblemDifficulty difficulty,
        Set<CodeLanguage> allowedLanguages,
        Map<CodeLanguage, String> starterCodes,
        List<TestCase> testCases
) {
    public static GeneratedProgrammingProblemDraftResponse from(GeneratedProgrammingProblemDraft draft) {
        return new GeneratedProgrammingProblemDraftResponse(
                draft.slug(), draft.title(), draft.summary(), draft.description(),
                draft.inputDescription(), draft.outputDescription(), draft.sampleInput(), draft.sampleOutput(),
                draft.topic(), draft.difficulty(), draft.allowedLanguages(), draft.starterCodes(),
                draft.testCases().stream()
                        .map(testCase -> new TestCase(
                                testCase.input(), testCase.expectedOutput(), testCase.timeLimitMillis()))
                        .toList()
        );
    }

    public record TestCase(String input, String expectedOutput, int timeLimitMillis) {
    }
}
