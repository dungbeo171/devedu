package com.devedu.learningplatform.infrastructure.persistence.problem;

import com.devedu.learningplatform.application.problemrecommendation.ProblemRecommendationPreferencePort;
import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ProblemDifficulty;
import com.devedu.learningplatform.domain.model.ProblemTopic;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.EnumMap;
import java.util.UUID;

@Repository
public class ProblemRecommendationPreferenceAdapter implements ProblemRecommendationPreferencePort {

    private static final String TOPIC = "TOPIC";
    private static final String DIFFICULTY = "DIFFICULTY";
    private static final String LANGUAGE = "LANGUAGE";

    private final SpringDataProblemRecommendationSignalRepository repository;
    private final Clock clock;

    public ProblemRecommendationPreferenceAdapter(
            SpringDataProblemRecommendationSignalRepository repository,
            Clock clock
    ) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Profile findByUserId(UUID userId) {
        var topics = new EnumMap<ProblemTopic, Long>(ProblemTopic.class);
        var difficulties = new EnumMap<ProblemDifficulty, Long>(ProblemDifficulty.class);
        var languages = new EnumMap<CodeLanguage, Long>(CodeLanguage.class);
        repository.findAllByIdUserId(userId).forEach(signal -> {
            var id = signal.getId();
            switch (id.getSignalType()) {
                case TOPIC -> put(topics, ProblemTopic.class, id.getSignalValue(), signal.getWeight());
                case DIFFICULTY -> put(difficulties, ProblemDifficulty.class, id.getSignalValue(), signal.getWeight());
                case LANGUAGE -> put(languages, CodeLanguage.class, id.getSignalValue(), signal.getWeight());
                default -> { }
            }
        });
        return new Profile(topics, difficulties, languages);
    }

    @Override
    @Transactional
    public void recordFilter(UUID userId, ProblemTopic topic, ProblemDifficulty difficulty, CodeLanguage language) {
        increment(userId, TOPIC, topic == null ? null : topic.name(), 1);
        increment(userId, DIFFICULTY, difficulty == null ? null : difficulty.name(), 1);
        increment(userId, LANGUAGE, language == null ? null : language.name(), 1);
    }

    @Override
    @Transactional
    public void recordProblemRun(
            UUID userId,
            ProblemTopic topic,
            ProblemDifficulty difficulty,
            CodeLanguage language
    ) {
        increment(userId, TOPIC, topic.name(), 2);
        increment(userId, DIFFICULTY, difficulty.name(), 1);
        increment(userId, LANGUAGE, language.name(), 2);
    }

    @Override
    @Transactional
    public void recordCompilerRun(UUID userId, CodeLanguage language) {
        increment(userId, LANGUAGE, language.name(), 1);
    }

    private void increment(UUID userId, String type, String value, long weight) {
        if (userId == null || value == null) return;
        repository.increment(userId, type, value, weight, clock.instant());
    }

    private <E extends Enum<E>> void put(
            EnumMap<E, Long> target,
            Class<E> type,
            String value,
            long weight
    ) {
        try {
            target.put(Enum.valueOf(type, value), weight);
        } catch (IllegalArgumentException ignored) {
            // Ignore retired enum values left by older deployments.
        }
    }
}
