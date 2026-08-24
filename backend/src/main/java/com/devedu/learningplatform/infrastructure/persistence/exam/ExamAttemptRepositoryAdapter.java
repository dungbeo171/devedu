package com.devedu.learningplatform.infrastructure.persistence.exam;

import com.devedu.learningplatform.application.port.out.ExamAttemptRepository;
import com.devedu.learningplatform.domain.model.ExamAttempt;
import org.springframework.stereotype.Repository;
import java.util.List; import java.util.Optional; import java.util.UUID;

@Repository
public class ExamAttemptRepositoryAdapter implements ExamAttemptRepository{
    private final SpringDataExamAttemptRepository repository;
    public ExamAttemptRepositoryAdapter(SpringDataExamAttemptRepository repository){this.repository=repository;}
    @Override public ExamAttempt save(ExamAttempt a){return toDomain(repository.saveAndFlush(new ExamAttemptJpaEntity(a.id(),a.examId(),a.studentId(),a.status(),a.startedAt(),a.expiresAt(),a.submittedAt(),a.automaticScore(),a.automaticMaxScore(),a.pendingCodingQuestions())));}
    @Override public Optional<ExamAttempt> findById(UUID id){return repository.findById(id).map(this::toDomain);}
    @Override public Optional<ExamAttempt> findByExamIdAndStudentId(UUID examId,UUID studentId){return repository.findByExamIdAndStudentId(examId,studentId).map(this::toDomain);}
    @Override public List<ExamAttempt> findAllByExamId(UUID examId){return repository.findAllByExamIdOrderByStartedAtDesc(examId).stream().map(this::toDomain).toList();}
    @Override public boolean existsByExamId(UUID examId){return repository.existsByExamId(examId);}
    private ExamAttempt toDomain(ExamAttemptJpaEntity e){return new ExamAttempt(e.getId(),e.getExamId(),e.getStudentId(),e.getStatus(),e.getStartedAt(),e.getExpiresAt(),e.getSubmittedAt(),e.getAutomaticScore(),e.getAutomaticMaxScore(),e.getPendingCodingQuestions());}
}
