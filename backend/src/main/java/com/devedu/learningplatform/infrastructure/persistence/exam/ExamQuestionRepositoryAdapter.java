package com.devedu.learningplatform.infrastructure.persistence.exam;

import com.devedu.learningplatform.application.port.out.ExamQuestionRepository;
import com.devedu.learningplatform.domain.model.ExamQuestion;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Repository
public class ExamQuestionRepositoryAdapter implements ExamQuestionRepository {
    private final SpringDataExamQuestionRepository questions;
    private final SpringDataExamQuestionOptionRepository options;

    public ExamQuestionRepositoryAdapter(
            SpringDataExamQuestionRepository questions,
            SpringDataExamQuestionOptionRepository options
    ) {
        this.questions = questions;
        this.options = options;
    }

    @Override
    @Transactional
    public ExamQuestion save(ExamQuestion question) {
        var entity = questions.saveAndFlush(new ExamQuestionJpaEntity(
                question.id(), question.examId(), question.type(), question.prompt(),
                question.correctOptionIndex(), question.codingLanguage(), question.points(),
                question.position(), question.createdAt()
        ));
        if (!question.options().isEmpty()) {
            options.saveAllAndFlush(IntStream.range(0, question.options().size())
                    .mapToObj(index -> new ExamQuestionOptionJpaEntity(
                            UUID.randomUUID(), question.id(), index, question.options().get(index)))
                    .toList());
        }
        return toDomain(entity, question.options());
    }

    @Override
    public Optional<ExamQuestion> findById(UUID id) {
        return questions.findById(id).map(entity -> toDomain(
                entity,
                options.findAllByQuestionIdOrderByOptionIndexAsc(id).stream()
                        .map(ExamQuestionOptionJpaEntity::getValue)
                        .toList()
        ));
    }

    @Override
    public List<ExamQuestion> findAllByExamId(UUID examId) {
        var entities = questions.findAllByExamIdOrderByPositionAsc(examId);
        if (entities.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<String>> valuesByQuestion = options
                .findAllByQuestionIdInOrderByQuestionIdAscOptionIndexAsc(
                        entities.stream().map(ExamQuestionJpaEntity::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(ExamQuestionOptionJpaEntity::getQuestionId,
                        Collectors.mapping(ExamQuestionOptionJpaEntity::getValue, Collectors.toList())));
        return entities.stream()
                .map(entity -> toDomain(entity, valuesByQuestion.getOrDefault(entity.getId(), List.of())))
                .toList();
    }

    private ExamQuestion toDomain(ExamQuestionJpaEntity entity, List<String> values) {
        return new ExamQuestion(
                entity.getId(), entity.getExamId(), entity.getType(), entity.getPrompt(), values,
                entity.getCorrectOptionIndex(), entity.getCodingLanguage(), entity.getPoints(),
                entity.getPosition(), entity.getCreatedAt()
        );
    }
}
