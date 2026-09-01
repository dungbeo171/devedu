package com.devedu.learningplatform.application.port.in.command;

import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ProblemDifficulty;
import com.devedu.learningplatform.domain.model.ProblemTopic;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record UpdateProgrammingProblemCommand(
        String currentSlug,
        String slug,
        String title,
        String summary,
        String description,
        String sampleInput,
        String sampleOutput,
        ProblemTopic topic,
        ProblemDifficulty difficulty,
        Set<CodeLanguage> allowedLanguages,
        Map<CodeLanguage, String> starterCodes,
        List<CreateProblemTestCaseCommand> testCases
) {
}
