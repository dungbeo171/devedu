package com.devedu.learningplatform.infrastructure.persistence.course;

import com.devedu.learningplatform.application.port.out.LessonProgressRepository;
import com.devedu.learningplatform.domain.model.LessonProgress;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class LessonProgressRepositoryAdapter implements LessonProgressRepository {
    private final SpringDataLessonProgressRepository repository;
    public LessonProgressRepositoryAdapter(SpringDataLessonProgressRepository repository) { this.repository = repository; }

    @Override public Optional<LessonProgress> findByStudentIdAndLessonId(UUID studentId, UUID lessonId) {
        return repository.findByStudentIdAndLessonId(studentId, lessonId).map(this::toDomain);
    }
    @Override public LessonProgress save(LessonProgress progress) {
        try {
            return toDomain(repository.saveAndFlush(new LessonProgressJpaEntity(
                    progress.id(), progress.studentId(), progress.lessonId(), progress.completedAt())));
        } catch (DataIntegrityViolationException exception) {
            return repository.findByStudentIdAndLessonId(progress.studentId(), progress.lessonId())
                    .map(this::toDomain)
                    .orElseThrow(() -> exception);
        }
    }
    private LessonProgress toDomain(LessonProgressJpaEntity entity) {
        return new LessonProgress(entity.getId(), entity.getStudentId(), entity.getLessonId(), entity.getCompletedAt());
    }
}
