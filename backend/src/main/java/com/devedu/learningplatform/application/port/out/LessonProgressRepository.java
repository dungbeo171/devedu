package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.domain.model.LessonProgress;

import java.util.Optional;
import java.util.UUID;

public interface LessonProgressRepository {

    Optional<LessonProgress> findByStudentIdAndLessonId(UUID studentId, UUID lessonId);

    LessonProgress save(LessonProgress progress);
}

