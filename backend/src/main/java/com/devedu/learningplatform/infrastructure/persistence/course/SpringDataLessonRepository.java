package com.devedu.learningplatform.infrastructure.persistence.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataLessonRepository extends JpaRepository<LessonJpaEntity, UUID> {
    List<LessonJpaEntity> findAllByTopicIdInOrderByPositionAsc(List<UUID> topicIds);
}
