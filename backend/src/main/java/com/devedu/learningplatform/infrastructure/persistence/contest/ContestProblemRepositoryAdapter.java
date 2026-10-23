package com.devedu.learningplatform.infrastructure.persistence.contest;

import com.devedu.learningplatform.application.contest.ContestProblemRepository;
import com.devedu.learningplatform.domain.contest.ContestProblemDefinition;
import com.devedu.learningplatform.domain.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Transactional(readOnly = true)
public class ContestProblemRepositoryAdapter implements ContestProblemRepository {
    private final EntityManager em;
    private final ObjectMapper json;

    public ContestProblemRepositoryAdapter(EntityManager em, ObjectMapper json) {
        this.em = em;
        this.json = json;
    }

    @Override
    @Transactional
    public ContestProblemDefinition save(ContestProblemDefinition value) {
        var problem = value.problem();
        var languages = problem.allowedLanguages().stream().map(Enum::name).sorted().collect(Collectors.joining(","));
        em.createNativeQuery("""
                insert into contest_problem_definitions
                (id,contest_id,slug,title,summary,description,input_description,output_description,sample_input,sample_output,topic,difficulty,allowed_languages,starter_codes,created_at)
                values (:id,:contest,:slug,:title,:summary,:description,:inputDescription,:outputDescription,:sampleInput,:sampleOutput,:topic,:difficulty,:languages,:starterCodes,:createdAt)
                on conflict (id) do update set slug=excluded.slug,title=excluded.title,summary=excluded.summary,
                description=excluded.description,input_description=excluded.input_description,output_description=excluded.output_description,
                sample_input=excluded.sample_input,sample_output=excluded.sample_output,topic=excluded.topic,difficulty=excluded.difficulty,
                allowed_languages=excluded.allowed_languages,starter_codes=excluded.starter_codes
                """).setParameter("id", problem.id()).setParameter("contest", value.contestId())
                .setParameter("slug", problem.slug()).setParameter("title", problem.title())
                .setParameter("summary", problem.summary()).setParameter("description", problem.description())
                .setParameter("inputDescription", problem.inputDescription()).setParameter("outputDescription", problem.outputDescription())
                .setParameter("sampleInput", problem.sampleInput()).setParameter("sampleOutput", problem.sampleOutput())
                .setParameter("topic", problem.topic().name()).setParameter("difficulty", problem.difficulty().name())
                .setParameter("languages", languages).setParameter("starterCodes", write(problem.starterCodes()))
                .setParameter("createdAt", problem.createdAt()).executeUpdate();
        em.createNativeQuery("delete from contest_problem_test_cases where problem_id=:problem")
                .setParameter("problem", problem.id()).executeUpdate();
        for (var test : value.testCases()) {
            em.createNativeQuery("insert into contest_problem_test_cases(id,problem_id,input,expected_output,time_limit_ms,position) values(:id,:problem,:input,:output,:limit,:position)")
                    .setParameter("id", test.id()).setParameter("problem", problem.id()).setParameter("input", test.input())
                    .setParameter("output", test.expectedOutput()).setParameter("limit", test.timeLimitMillis())
                    .setParameter("position", test.position()).executeUpdate();
        }
        return value;
    }

    @Override
    public Optional<ContestProblemDefinition> find(UUID contestId, UUID problemId) {
        var rows = em.createNativeQuery(baseSelect() + " where p.contest_id=:contest and p.id=:problem")
                .setParameter("contest", contestId).setParameter("problem", problemId).getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(map((Object[]) rows.get(0)));
    }

    @Override
    public List<ContestProblemDefinition> findAll(UUID contestId) {
        return em.createNativeQuery(baseSelect() + " where p.contest_id=:contest order by p.created_at,p.id")
                .setParameter("contest", contestId).getResultList().stream()
                .map(row -> map((Object[]) row)).toList();
    }

    private String baseSelect() {
        return "select p.id,p.contest_id,p.slug,p.title,p.summary,p.description,p.input_description,p.output_description,p.sample_input,p.sample_output,p.topic,p.difficulty,p.allowed_languages,p.starter_codes,p.created_at from contest_problem_definitions p";
    }

    private ContestProblemDefinition map(Object[] row) {
        var id = (UUID) row[0];
        var problem = new ProgrammingProblem(id, (String) row[2], (String) row[3], (String) row[4], (String) row[5],
                (String) row[6], (String) row[7], (String) row[8], (String) row[9], ProblemTopic.valueOf((String) row[10]),
                ProblemDifficulty.valueOf((String) row[11]), Arrays.stream(((String) row[12]).split(","))
                .filter(value -> !value.isBlank()).map(CodeLanguage::valueOf).collect(Collectors.toUnmodifiableSet()),
                read((String) row[13]), instant(row[14]));
        @SuppressWarnings("unchecked")
        List<Object[]> testRows = em.createNativeQuery("select id,input,expected_output,time_limit_ms,position from contest_problem_test_cases where problem_id=:problem order by position")
                .setParameter("problem", id).getResultList();
        var tests = testRows.stream().map(test -> new ProblemTestCase((UUID) test[0], id, (String) test[1], (String) test[2],
                ((Number) test[3]).intValue(), ((Number) test[4]).intValue())).toList();
        return new ContestProblemDefinition((UUID) row[1], problem, tests);
    }

    private String write(Map<CodeLanguage, String> value) {
        try { return json.writeValueAsString(value); }
        catch (Exception exception) { throw new IllegalStateException("Could not serialize contest problem starter code", exception); }
    }

    private Map<CodeLanguage, String> read(String value) {
        try { return json.readValue(value, new TypeReference<>() {}); }
        catch (Exception exception) { throw new IllegalStateException("Could not deserialize contest problem starter code", exception); }
    }

    private Instant instant(Object value) {
        if (value instanceof Instant instant) return instant;
        if (value instanceof OffsetDateTime date) return date.toInstant();
        return ((java.sql.Timestamp) value).toInstant();
    }
}
