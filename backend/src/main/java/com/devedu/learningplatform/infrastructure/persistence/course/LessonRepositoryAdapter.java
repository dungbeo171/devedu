package com.devedu.learningplatform.infrastructure.persistence.course;

import com.devedu.learningplatform.application.port.out.LessonRepository;
import com.devedu.learningplatform.domain.model.Lesson;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class LessonRepositoryAdapter implements LessonRepository {
    private final SpringDataLessonRepository repository;
    public LessonRepositoryAdapter(SpringDataLessonRepository repository) { this.repository = repository; }

    @Override public Lesson save(Lesson lesson) {
        return toDomain(repository.saveAndFlush(new LessonJpaEntity(lesson.id(), lesson.topicId(), lesson.title(),
                lesson.content(), lesson.videoUrl(), lesson.position(), lesson.createdAt())));
    }
    @Override public Optional<Lesson> findById(UUID id) { return repository.findById(id).map(this::toDomain); }
    @Override public List<Lesson> findAllByTopicIds(List<UUID> topicIds) {
        return repository.findAllByTopicIdInOrderByPositionAsc(topicIds).stream().map(this::toDomain).toList();
    }
    private Lesson toDomain(LessonJpaEntity entity) {
        return new Lesson(entity.getId(), entity.getTopicId(), entity.getTitle(), entity.getContent(), entity.getVideoUrl(), entity.getPosition(), entity.getCreatedAt());
    }
}
