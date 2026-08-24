package com.devedu.learningplatform.infrastructure.persistence.exam;

import com.devedu.learningplatform.application.port.out.ExamAnswerRepository;
import com.devedu.learningplatform.domain.model.ExamAnswer;
import org.springframework.stereotype.Repository;
import java.util.List; import java.util.Optional; import java.util.UUID;

@Repository
public class ExamAnswerRepositoryAdapter implements ExamAnswerRepository{
    private final SpringDataExamAnswerRepository repository;
    public ExamAnswerRepositoryAdapter(SpringDataExamAnswerRepository repository){this.repository=repository;}
    @Override public ExamAnswer save(ExamAnswer a){return toDomain(repository.saveAndFlush(new ExamAnswerJpaEntity(a.id(),a.attemptId(),a.questionId(),a.selectedOptionIndex(),a.sourceCode(),a.answeredAt())));}
    @Override public Optional<ExamAnswer> findByAttemptIdAndQuestionId(UUID attemptId,UUID questionId){return repository.findByAttemptIdAndQuestionId(attemptId,questionId).map(this::toDomain);}
    @Override public List<ExamAnswer> findAllByAttemptId(UUID attemptId){return repository.findAllByAttemptIdOrderByAnsweredAtAsc(attemptId).stream().map(this::toDomain).toList();}
    private ExamAnswer toDomain(ExamAnswerJpaEntity e){return new ExamAnswer(e.getId(),e.getAttemptId(),e.getQuestionId(),e.getSelectedOptionIndex(),e.getSourceCode(),e.getAnsweredAt());}
}
