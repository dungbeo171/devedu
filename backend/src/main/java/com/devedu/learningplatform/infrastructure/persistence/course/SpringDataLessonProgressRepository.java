package com.devedu.learningplatform.infrastructure.persistence.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataLessonProgressRepository extends JpaRepository<LessonProgressJpaEntity, UUID> {
    Optional<LessonProgressJpaEntity> findByStudentIdAndLessonId(UUID studentId, UUID lessonId);
}
