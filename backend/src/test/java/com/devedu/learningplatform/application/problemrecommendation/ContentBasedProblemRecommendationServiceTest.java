package com.devedu.learningplatform.application.problemrecommendation;

import com.devedu.learningplatform.application.port.out.ProblemRunStatisticsRepository;
import com.devedu.learningplatform.application.port.out.ProgrammingProblemRepository;
import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ProblemDifficulty;
import com.devedu.learningplatform.domain.model.ProblemRunStatistics;
import com.devedu.learningplatform.domain.model.ProblemTestCase;
import com.devedu.learningplatform.domain.model.ProblemTopic;
import com.devedu.learningplatform.domain.model.ProgrammingProblem;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ContentBasedProblemRecommendationServiceTest {

    private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final ProgrammingProblem SOLVED = problem("mang-co-ban", ProblemTopic.DATA_STRUCTURES, ProblemDifficulty.EASY);
    private static final ProgrammingProblem RELATED = problem("stack", ProblemTopic.DATA_STRUCTURES, ProblemDifficulty.MEDIUM);
    private static final ProgrammingProblem UNRELATED = problem("truy-van-sql", ProblemTopic.SQL, ProblemDifficulty.MEDIUM);
    private static final ProgrammingProblem INTRO = problem("tong-hai-so", ProblemTopic.INTRODUCTION, ProblemDifficulty.EASY);

    @Test
    void recommendsUnsolvedProblemsThatMatchTheUsersContentHistory() {
        var service = service(List.of(
                new ProblemRecommendationSignalsPort.Activity(
                        SOLVED.id(), CodeLanguage.JAVA, 2, 1, Instant.parse("2026-10-20T10:00:00Z")
                )
        ));

        var recommendations = service.recommend(USER_ID, null, null, null);

        assertThat(recommendations.get(0).problem()).isEqualTo(RELATED);
        assertThat(recommendations).extracting(item -> item.problem().id()).contains(SOLVED.id());
        assertThat(recommendations.get(recommendations.size() - 1).problem()).isEqualTo(SOLVED);
    }

    @Test
    void givesNewUsersAnEasyIntroductionFallback() {
        var recommendations = service(List.of()).recommend(null, null, null, null);

        assertThat(recommendations.get(0).problem()).isEqualTo(INTRO);
    }

    @Test
    void appliesTheSelectedCatalogFilters() {
        var recommendations = service(List.of()).recommend(
                USER_ID, ProblemTopic.SQL, ProblemDifficulty.MEDIUM, CodeLanguage.JAVA
        );

        assertThat(recommendations).allSatisfy(item -> {
            assertThat(item.problem().topic()).isEqualTo(ProblemTopic.SQL);
            assertThat(item.problem().difficulty()).isEqualTo(ProblemDifficulty.MEDIUM);
        });
    }

    @Test
    void usesStoredTopicPreferencesInTheListOrder() {
        var profile = new ProblemRecommendationPreferencePort.Profile(
                Map.of(ProblemTopic.SQL, 8L), Map.of(), Map.of()
        );

        var recommendations = service(List.of(), profile).recommend(USER_ID, null, null, null);

        assertThat(recommendations.get(0).problem()).isEqualTo(UNRELATED);
    }

    private ContentBasedProblemRecommendationService service(
            List<ProblemRecommendationSignalsPort.Activity> activities
    ) {
        return service(activities, ProblemRecommendationPreferencePort.Profile.empty());
    }

    private ContentBasedProblemRecommendationService service(
            List<ProblemRecommendationSignalsPort.Activity> activities,
            ProblemRecommendationPreferencePort.Profile profile
    ) {
        var catalog = List.of(SOLVED, RELATED, UNRELATED, INTRO);
        ProgrammingProblemRepository repository = new ProgrammingProblemRepository() {
            @Override public List<ProgrammingProblem> findAll(ProblemTopic topic, ProblemDifficulty difficulty, CodeLanguage language) {
                return catalog.stream()
                        .filter(problem -> topic == null || problem.topic() == topic)
                        .filter(problem -> difficulty == null || problem.difficulty() == difficulty)
                        .filter(problem -> language == null || problem.allowedLanguages().contains(language))
                        .toList();
            }
            @Override public Optional<ProgrammingProblem> findBySlug(String slug) { return Optional.empty(); }
            @Override public boolean existsBySlug(String slug) { return false; }
            @Override public ProgrammingProblem saveWithTestCases(ProgrammingProblem problem, List<ProblemTestCase> testCases) { return problem; }
            @Override public void deleteById(UUID problemId) { }
        };
        ProblemRunStatisticsRepository statistics = new ProblemRunStatisticsRepository() {
            @Override public void record(UUID problemId, boolean successful) { }
            @Override public Map<UUID, ProblemRunStatistics> findAllByProblemIds(Set<UUID> problemIds) { return Map.of(); }
        };
        return new ContentBasedProblemRecommendationService(
                repository,
                statistics,
                ignored -> activities,
                new ProblemRecommendationPreferencePort() {
                    @Override public Profile findByUserId(UUID userId) { return profile; }
                    @Override public void recordFilter(UUID userId, ProblemTopic topic, ProblemDifficulty difficulty, CodeLanguage language) { }
                    @Override public void recordProblemRun(UUID userId, ProblemTopic topic, ProblemDifficulty difficulty, CodeLanguage language) { }
                    @Override public void recordCompilerRun(UUID userId, CodeLanguage language) { }
                }
        );
    }

    private static ProgrammingProblem problem(String slug, ProblemTopic topic, ProblemDifficulty difficulty) {
        return new ProgrammingProblem(
                UUID.nameUUIDFromBytes(slug.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                slug, slug, "Tóm tắt", "Mô tả", "Input", "Output", "1", "1",
                topic, difficulty, Set.of(CodeLanguage.JAVA), Map.of(CodeLanguage.JAVA, "class Main {}"),
                Instant.parse("2026-10-01T00:00:00Z")
        );
    }
}
