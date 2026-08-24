package com.devedu.learningplatform.infrastructure.persistence.exam;

import com.devedu.learningplatform.application.exception.ExamSlugAlreadyExistsException;
import com.devedu.learningplatform.application.port.out.ExamRepository;
import com.devedu.learningplatform.domain.model.Exam;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import java.util.List; import java.util.Optional; import java.util.UUID;

@Repository
public class ExamRepositoryAdapter implements ExamRepository {
    private final SpringDataExamRepository repository;
    public ExamRepositoryAdapter(SpringDataExamRepository repository){this.repository=repository;}
    @Override public boolean existsBySlug(String slug){return repository.existsBySlug(slug);}
    @Override public Exam save(Exam exam){
        try { return toDomain(repository.saveAndFlush(new ExamJpaEntity(exam.id(),exam.slug(),exam.title(),exam.description(),exam.teacherId(),exam.scheduledAt(),exam.durationMinutes(),exam.createdAt()))); }
        catch (DataIntegrityViolationException exception){throw new ExamSlugAlreadyExistsException(exam.slug());}
    }
    @Override public List<Exam> findAll(){return repository.findAllByOrderByScheduledAtAsc().stream().map(this::toDomain).toList();}
    @Override public List<Exam> findAllByTeacherId(UUID teacherId){return repository.findAllByTeacherIdOrderByScheduledAtAsc(teacherId).stream().map(this::toDomain).toList();}
    @Override public Optional<Exam> findById(UUID id){return repository.findById(id).map(this::toDomain);}
    @Override public Optional<Exam> findBySlug(String slug){return repository.findBySlug(slug).map(this::toDomain);}
    private Exam toDomain(ExamJpaEntity e){return new Exam(e.getId(),e.getSlug(),e.getTitle(),e.getDescription(),e.getTeacherId(),e.getScheduledAt(),e.getDurationMinutes(),e.getCreatedAt());}
}
