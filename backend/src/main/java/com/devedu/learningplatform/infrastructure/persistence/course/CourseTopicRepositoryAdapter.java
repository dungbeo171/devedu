package com.devedu.learningplatform.infrastructure.persistence.course;

import com.devedu.learningplatform.application.port.out.CourseTopicRepository;
import com.devedu.learningplatform.domain.model.CourseTopic;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CourseTopicRepositoryAdapter implements CourseTopicRepository {
    private final SpringDataCourseTopicRepository repository;
    public CourseTopicRepositoryAdapter(SpringDataCourseTopicRepository repository) { this.repository = repository; }

    @Override public CourseTopic save(CourseTopic topic) {
        return toDomain(repository.saveAndFlush(new CourseTopicJpaEntity(topic.id(), topic.courseId(), topic.title(), topic.position(), topic.createdAt())));
    }
    @Override public Optional<CourseTopic> findById(UUID id) { return repository.findById(id).map(this::toDomain); }
    @Override public List<CourseTopic> findAllByCourseId(UUID courseId) {
        return repository.findAllByCourseIdOrderByPositionAsc(courseId).stream().map(this::toDomain).toList();
    }
    private CourseTopic toDomain(CourseTopicJpaEntity entity) {
        return new CourseTopic(entity.getId(), entity.getCourseId(), entity.getTitle(), entity.getPosition(), entity.getCreatedAt());
    }
}
