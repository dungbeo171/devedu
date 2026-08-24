package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.domain.model.Exam;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamRepository {
    boolean existsBySlug(String slug);
    Exam save(Exam exam);
    List<Exam> findAll();
    List<Exam> findAllByTeacherId(UUID teacherId);
    Optional<Exam> findById(UUID id);
    Optional<Exam> findBySlug(String slug);
}
