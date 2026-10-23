package com.devedu.learningplatform.application.problemrecommendation;

import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ProblemDifficulty;
import com.devedu.learningplatform.domain.model.ProblemTopic;

import java.util.Map;
import java.util.UUID;

public interface ProblemRecommendationPreferencePort {

    static ProblemRecommendationPreferencePort noop() {
        return new ProblemRecommendationPreferencePort() {
            @Override public Profile findByUserId(UUID userId) { return Profile.empty(); }
            @Override public void recordFilter(UUID userId, ProblemTopic topic, ProblemDifficulty difficulty, CodeLanguage language) { }
            @Override public void recordProblemRun(UUID userId, ProblemTopic topic, ProblemDifficulty difficulty, CodeLanguage language) { }
            @Override public void recordCompilerRun(UUID userId, CodeLanguage language) { }
        };
    }

    Profile findByUserId(UUID userId);

    void recordFilter(UUID userId, ProblemTopic topic, ProblemDifficulty difficulty, CodeLanguage language);

    void recordProblemRun(
            UUID userId,
            ProblemTopic topic,
            ProblemDifficulty difficulty,
            CodeLanguage language
    );

    void recordCompilerRun(UUID userId, CodeLanguage language);

    record Profile(
            Map<ProblemTopic, Long> topics,
            Map<ProblemDifficulty, Long> difficulties,
            Map<CodeLanguage, Long> languages
    ) {
        public Profile {
            topics = Map.copyOf(topics);
            difficulties = Map.copyOf(difficulties);
            languages = Map.copyOf(languages);
        }

        public static Profile empty() {
            return new Profile(Map.of(), Map.of(), Map.of());
        }
    }
}
