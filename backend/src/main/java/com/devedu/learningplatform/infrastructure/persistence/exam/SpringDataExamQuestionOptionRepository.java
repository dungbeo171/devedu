package com.devedu.learningplatform.infrastructure.persistence.exam;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.UUID;

interface SpringDataExamQuestionOptionRepository extends JpaRepository<ExamQuestionOptionJpaEntity,UUID>{
    List<ExamQuestionOptionJpaEntity> findAllByQuestionIdOrderByOptionIndexAsc(UUID questionId);
    List<ExamQuestionOptionJpaEntity> findAllByQuestionIdInOrderByQuestionIdAscOptionIndexAsc(List<UUID> questionIds);
}
