package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.domain.model.ExamQuestion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamQuestionRepository {
    ExamQuestion save(ExamQuestion question);
    Optional<ExamQuestion> findById(UUID id);
    List<ExamQuestion> findAllByExamId(UUID examId);
}
