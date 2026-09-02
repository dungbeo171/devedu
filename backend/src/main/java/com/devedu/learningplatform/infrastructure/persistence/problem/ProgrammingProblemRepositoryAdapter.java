package com.devedu.learningplatform.infrastructure.persistence.problem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.devedu.learningplatform.application.port.out.ProgrammingProblemRepository;
import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ProblemDifficulty;
import com.devedu.learningplatform.domain.model.ProblemTopic;
import com.devedu.learningplatform.domain.model.ProgrammingProblem;
import com.devedu.learningplatform.domain.model.ProblemTestCase;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Arrays;
import java.util.stream.Collectors;

@Repository
public class ProgrammingProblemRepositoryAdapter implements ProgrammingProblemRepository {

    private final SpringDataProgrammingProblemRepository repository;
    private final SpringDataProblemTestCaseRepository testCaseRepository;
    private final ObjectMapper objectMapper;

    public ProgrammingProblemRepositoryAdapter(SpringDataProgrammingProblemRepository repository,
                                               SpringDataProblemTestCaseRepository testCaseRepository,
                                               ObjectMapper objectMapper) {
        this.repository = repository;
        this.testCaseRepository = testCaseRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ProgrammingProblem> findAll(ProblemTopic topic, ProblemDifficulty difficulty, CodeLanguage language) {
        return repository.findAllFiltered(topic, difficulty, language == null ? "" : language.name())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<ProgrammingProblem> findBySlug(String slug) {
        return repository.findBySlugAndDeletedFalse(slug).map(this::toDomain);
    }

    @Override
    public Optional<ProgrammingProblem> findById(java.util.UUID id) {
        return repository.findByIdAndDeletedFalse(id).map(this::toDomain);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return repository.existsBySlug(slug);
    }

    @Override
    @Transactional
    public ProgrammingProblem saveWithTestCases(ProgrammingProblem problem, List<ProblemTestCase> testCases) {
        var saved = repository.save(toEntity(problem));
        testCaseRepository.deleteAllByProblemId(problem.id());
        testCaseRepository.saveAll(testCases.stream().map(this::toEntity).toList());
        return toDomain(saved);
    }

    @Override
    @Transactional
    public void deleteById(java.util.UUID problemId) {
        repository.softDeleteById(problemId);
    }

    private ProgrammingProblemJpaEntity toEntity(ProgrammingProblem problem) {
        var languages = problem.allowedLanguages().stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(","));
        return new ProgrammingProblemJpaEntity(
                problem.id(), problem.slug(), problem.title(), problem.summary(), problem.description(),
                problem.sampleInput(), problem.sampleOutput(), problem.topic(), problem.difficulty(),
                languages, serializeStarterCodes(problem.starterCodes()), problem.createdAt(), false
        );
    }

    private ProblemTestCaseJpaEntity toEntity(ProblemTestCase testCase) {
        return new ProblemTestCaseJpaEntity(
                testCase.id(), testCase.problemId(), testCase.input(), testCase.expectedOutput(),
                testCase.timeLimitMillis(), testCase.position()
        );
    }

    private ProgrammingProblem toDomain(ProgrammingProblemJpaEntity entity) {
        return new ProgrammingProblem(
                entity.getId(),
                entity.getSlug(),
                entity.getTitle(),
                entity.getSummary(),
                entity.getDescription(),
                entity.getSampleInput(),
                entity.getSampleOutput(),
                entity.getTopic(),
                entity.getDifficulty(),
                Arrays.stream(entity.getAllowedLanguages().split(","))
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .map(CodeLanguage::valueOf)
                        .collect(Collectors.toUnmodifiableSet()),
                deserializeStarterCodes(entity.getStarterCodes()),
                entity.getCreatedAt()
        );
    }

    private String serializeStarterCodes(Map<CodeLanguage, String> starterCodes) {
        try {
            return objectMapper.writeValueAsString(starterCodes);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize problem starter code", exception);
        }
    }

    private Map<CodeLanguage, String> deserializeStarterCodes(String starterCodes) {
        try {
            return objectMapper.readValue(starterCodes, new TypeReference<>() { });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not deserialize problem starter code", exception);
        }
    }
}
