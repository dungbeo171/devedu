package com.devedu.learningplatform.infrastructure.persistence.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataCourseTopicRepository extends JpaRepository<CourseTopicJpaEntity, UUID> {
    List<CourseTopicJpaEntity> findAllByCourseIdOrderByPositionAsc(UUID courseId);
}
