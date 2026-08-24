package com.devedu.learningplatform.infrastructure.persistence.exam;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.UUID;

interface SpringDataExamQuestionRepository extends JpaRepository<ExamQuestionJpaEntity,UUID>{
    List<ExamQuestionJpaEntity> findAllByExamIdOrderByPositionAsc(UUID examId);
}
