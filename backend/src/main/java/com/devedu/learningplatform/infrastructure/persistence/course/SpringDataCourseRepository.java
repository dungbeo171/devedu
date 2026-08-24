package com.devedu.learningplatform.infrastructure.persistence.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataCourseRepository extends JpaRepository<CourseJpaEntity, UUID> {
    boolean existsBySlug(String slug);
    Optional<CourseJpaEntity> findBySlug(String slug);
    List<CourseJpaEntity> findAllByOrderByTitleAsc();
}
