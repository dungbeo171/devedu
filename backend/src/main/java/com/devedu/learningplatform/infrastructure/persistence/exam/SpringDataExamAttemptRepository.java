package com.devedu.learningplatform.infrastructure.persistence.exam;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.Optional; import java.util.UUID;

interface SpringDataExamAttemptRepository extends JpaRepository<ExamAttemptJpaEntity,UUID>{
    Optional<ExamAttemptJpaEntity> findByExamIdAndStudentId(UUID examId,UUID studentId);
    List<ExamAttemptJpaEntity> findAllByExamIdOrderByStartedAtDesc(UUID examId);
    boolean existsByExamId(UUID examId);
}
