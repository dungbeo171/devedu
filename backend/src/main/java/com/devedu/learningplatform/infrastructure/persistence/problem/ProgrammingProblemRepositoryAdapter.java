package com.devedu.learningplatform.infrastructure.persistence.problem;

import com.devedu.learningplatform.application.port.out.ProgrammingProblemRepository;
import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ProblemDifficulty;
import com.devedu.learningplatform.domain.model.ProblemTopic;
import com.devedu.learningplatform.domain.model.ProgrammingProblem;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Arrays;
import java.util.stream.Collectors;

@Repository
public class ProgrammingProblemRepositoryAdapter implements ProgrammingProblemRepository {

    private final SpringDataProgrammingProblemRepository repository;

    public ProgrammingProblemRepositoryAdapter(SpringDataProgrammingProblemRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ProgrammingProblem> findAll(ProblemTopic topic, ProblemDifficulty difficulty, CodeLanguage language) {
        return repository.findAllFiltered(topic, difficulty, language == null ? "" : language.name())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<ProgrammingProblem> findBySlug(String slug) {
        return repository.findBySlug(slug).map(this::toDomain);
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
                entity.getCreatedAt()
        );
    }
}
