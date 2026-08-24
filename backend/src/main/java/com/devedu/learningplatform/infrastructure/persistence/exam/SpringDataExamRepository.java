package com.devedu.learningplatform.infrastructure.persistence.exam;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.Optional; import java.util.UUID;

interface SpringDataExamRepository extends JpaRepository<ExamJpaEntity, UUID> {
    boolean existsBySlug(String slug);
    Optional<ExamJpaEntity> findBySlug(String slug);
    List<ExamJpaEntity> findAllByOrderByScheduledAtAsc();
    List<ExamJpaEntity> findAllByTeacherIdOrderByScheduledAtAsc(UUID teacherId);
}
