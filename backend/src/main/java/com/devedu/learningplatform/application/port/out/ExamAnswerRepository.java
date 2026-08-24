package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.domain.model.ExamAnswer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamAnswerRepository {
    ExamAnswer save(ExamAnswer answer);
    Optional<ExamAnswer> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);
    List<ExamAnswer> findAllByAttemptId(UUID attemptId);
}
