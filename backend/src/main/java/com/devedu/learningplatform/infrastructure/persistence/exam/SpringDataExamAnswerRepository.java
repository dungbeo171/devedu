package com.devedu.learningplatform.infrastructure.persistence.exam;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.Optional; import java.util.UUID;

interface SpringDataExamAnswerRepository extends JpaRepository<ExamAnswerJpaEntity,UUID>{
    Optional<ExamAnswerJpaEntity> findByAttemptIdAndQuestionId(UUID attemptId,UUID questionId);
    List<ExamAnswerJpaEntity> findAllByAttemptIdOrderByAnsweredAtAsc(UUID attemptId);
}
