package com.devedu.learningplatform.infrastructure.persistence.interview;

import com.devedu.learningplatform.application.port.out.InterviewQuestionRepository;
import com.devedu.learningplatform.domain.model.InterviewDifficulty;
import com.devedu.learningplatform.domain.model.InterviewQuestion;
import com.devedu.learningplatform.domain.model.InterviewTopic;
import org.springframework.stereotype.Repository;
import java.util.List; import java.util.Optional; import java.util.UUID;

@Repository
public class InterviewQuestionRepositoryAdapter implements InterviewQuestionRepository {
    private final SpringDataInterviewQuestionRepository repository;
    public InterviewQuestionRepositoryAdapter(SpringDataInterviewQuestionRepository repository) { this.repository = repository; }
    @Override public List<InterviewQuestion> findAll(InterviewTopic topic, InterviewDifficulty difficulty) {
        var entities = topic == null && difficulty == null ? repository.findAllByOrderByQuestionAsc()
                : topic != null && difficulty == null ? repository.findAllByTopicOrderByQuestionAsc(topic)
                : topic == null ? repository.findAllByDifficultyOrderByQuestionAsc(difficulty)
                : repository.findAllByTopicAndDifficultyOrderByQuestionAsc(topic, difficulty);
        return entities.stream().map(this::toDomain).toList();
    }
    @Override public Optional<InterviewQuestion> findById(UUID id) { return repository.findById(id).map(this::toDomain); }
    private InterviewQuestion toDomain(InterviewQuestionJpaEntity entity) { return new InterviewQuestion(entity.getId(), entity.getQuestion(), entity.getAnswer(), entity.getExplanation(), entity.getDifficulty(), entity.getTopic(), entity.getCreatedAt()); }
}
