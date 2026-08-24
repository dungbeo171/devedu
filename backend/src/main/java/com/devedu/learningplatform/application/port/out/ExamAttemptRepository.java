package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.domain.model.ExamAttempt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamAttemptRepository {
    ExamAttempt save(ExamAttempt attempt);
    Optional<ExamAttempt> findById(UUID id);
    Optional<ExamAttempt> findByExamIdAndStudentId(UUID examId, UUID studentId);
    List<ExamAttempt> findAllByExamId(UUID examId);
    boolean existsByExamId(UUID examId);
}
