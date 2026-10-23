package com.devedu.learningplatform.application.problemrecommendation;

import com.devedu.learningplatform.application.port.out.ProblemRunStatisticsRepository;
import com.devedu.learningplatform.application.port.out.ProgrammingProblemRepository;
import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ProblemDifficulty;
import com.devedu.learningplatform.domain.model.ProblemRunStatistics;
import com.devedu.learningplatform.domain.model.ProblemTopic;
import com.devedu.learningplatform.domain.model.ProgrammingProblem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ContentBasedProblemRecommendationService implements ProblemRecommendationUseCase {

    private final ProgrammingProblemRepository problems;
    private final ProblemRunStatisticsRepository runStatistics;
    private final ProblemRecommendationSignalsPort signals;
    private final ProblemRecommendationPreferencePort preferences;

    public ContentBasedProblemRecommendationService(
            ProgrammingProblemRepository problems,
            ProblemRunStatisticsRepository runStatistics,
            ProblemRecommendationSignalsPort signals,
            ProblemRecommendationPreferencePort preferences
    ) {
        this.problems = Objects.requireNonNull(problems);
        this.runStatistics = Objects.requireNonNull(runStatistics);
        this.signals = Objects.requireNonNull(signals);
        this.preferences = Objects.requireNonNull(preferences);
    }

    @Override
    public List<ProblemRecommendationView> recommend(
            UUID userId,
            ProblemTopic selectedTopic,
            ProblemDifficulty selectedDifficulty,
            CodeLanguage selectedLanguage
    ) {
        var catalog = problems.findAll(selectedTopic, selectedDifficulty, selectedLanguage);
        if (catalog.isEmpty()) return List.of();

        var statistics = runStatistics.findAllByProblemIds(catalog.stream()
                .map(ProgrammingProblem::id)
                .collect(Collectors.toSet()));
        var activities = userId == null ? List.<ProblemRecommendationSignalsPort.Activity>of() : signals.findByUserId(userId);
        var storedPreferences = userId == null
                ? ProblemRecommendationPreferencePort.Profile.empty()
                : preferences.findByUserId(userId);
        if (userId != null) {
            preferences.recordFilter(userId, selectedTopic, selectedDifficulty, selectedLanguage);
        }
        var byId = catalog.stream().collect(Collectors.toMap(ProgrammingProblem::id, problem -> problem));

        var solved = new HashSet<UUID>();
        var attempted = new HashSet<UUID>();
        var topicAffinity = new EnumMap<ProblemTopic, Integer>(ProblemTopic.class);
        var languageAffinity = new EnumMap<CodeLanguage, Integer>(CodeLanguage.class);
        var solvedByDifficulty = new EnumMap<ProblemDifficulty, Integer>(ProblemDifficulty.class);

        for (var activity : activities) {
            var problem = byId.get(activity.problemId());
            if (problem == null) continue;
            attempted.add(problem.id());
            var engagement = 1 + (activity.solved() ? 2 : 0);
            topicAffinity.merge(problem.topic(), engagement, Integer::sum);
            languageAffinity.merge(activity.language(), engagement, Integer::sum);
            if (activity.solved()) {
                solved.add(problem.id());
                solvedByDifficulty.merge(problem.difficulty(), 1, Integer::sum);
            }
        }

        storedPreferences.topics().forEach((topic, weight) ->
                topicAffinity.merge(topic, boundedWeight(weight), Integer::sum));
        storedPreferences.languages().forEach((language, weight) ->
                languageAffinity.merge(language, boundedWeight(weight), Integer::sum));

        var hasKnownActivity = !attempted.isEmpty()
                || !storedPreferences.topics().isEmpty()
                || !storedPreferences.difficulties().isEmpty()
                || !storedPreferences.languages().isEmpty();
        var targetDifficulty = targetDifficulty(solvedByDifficulty);
        var ranked = new ArrayList<ScoredProblem>();
        for (var problem : catalog) {
            var acceptanceRate = statistics.getOrDefault(
                    problem.id(), new ProblemRunStatistics(problem.id(), 0, 0)
            ).acceptanceRate();
            var score = acceptanceRate / 10.0;

            if (!hasKnownActivity) {
                if (problem.difficulty() == ProblemDifficulty.EASY) score += 40;
                if (problem.topic() == ProblemTopic.INTRODUCTION) score += 20;
            } else {
                score += Math.min(topicAffinity.getOrDefault(problem.topic(), 0) * 12, 60);
                score += Math.min(problem.allowedLanguages().stream()
                        .mapToInt(language -> languageAffinity.getOrDefault(language, 0))
                        .max().orElse(0) * 7, 35);
                if (problem.difficulty() == targetDifficulty) score += 24;
                score += Math.min(storedPreferences.difficulties()
                        .getOrDefault(problem.difficulty(), 0L) * 5, 30);
                if (attempted.contains(problem.id())) score += 30;
            }
            if (solved.contains(problem.id())) score -= 1_000;
            ranked.add(new ScoredProblem(problem, acceptanceRate, score));
        }

        ranked.sort(Comparator.comparingDouble(ScoredProblem::score).reversed()
                .thenComparing(item -> item.problem().createdAt(), Comparator.reverseOrder())
                .thenComparing(item -> item.problem().slug()));

        // Keep the first screen useful instead of filling it with only one familiar topic.
        var selected = new ArrayList<ScoredProblem>();
        var perTopic = new HashMap<ProblemTopic, Integer>();
        for (var candidate : ranked) {
            if (solved.contains(candidate.problem().id())) continue;
            if (perTopic.getOrDefault(candidate.problem().topic(), 0) >= 3) continue;
            selected.add(candidate);
            perTopic.merge(candidate.problem().topic(), 1, Integer::sum);
        }
        for (var candidate : ranked) {
            if (!solved.contains(candidate.problem().id()) && !selected.contains(candidate)) selected.add(candidate);
        }
        for (var candidate : ranked) {
            if (solved.contains(candidate.problem().id())) selected.add(candidate);
        }
        return selected.stream()
                .map(item -> new ProblemRecommendationView(item.problem(), item.acceptanceRate()))
                .toList();
    }

    private int boundedWeight(long weight) {
        return (int) Math.min(Math.max(weight, 0), 10);
    }

    private ProblemDifficulty targetDifficulty(Map<ProblemDifficulty, Integer> solved) {
        if (solved.getOrDefault(ProblemDifficulty.MEDIUM, 0) >= 3
                || solved.getOrDefault(ProblemDifficulty.HARD, 0) > 0) return ProblemDifficulty.HARD;
        if (solved.getOrDefault(ProblemDifficulty.EASY, 0) >= 3
                || solved.getOrDefault(ProblemDifficulty.MEDIUM, 0) > 0) return ProblemDifficulty.MEDIUM;
        return ProblemDifficulty.EASY;
    }

    private record ScoredProblem(
            ProgrammingProblem problem,
            double acceptanceRate,
            double score
    ) {
    }
}
