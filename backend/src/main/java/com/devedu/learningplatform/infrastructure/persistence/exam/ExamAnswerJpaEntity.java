package com.devedu.learningplatform.infrastructure.persistence.exam;

import jakarta.persistence.*;
import java.time.Instant; import java.util.UUID;

@Entity @Table(name="exam_answers")
class ExamAnswerJpaEntity {
    @Id private UUID id;
    @Column(name="attempt_id",nullable=false) private UUID attemptId;
    @Column(name="question_id",nullable=false) private UUID questionId;
    @Column(name="selected_option_index") private Integer selectedOptionIndex;
    @Column(name="source_code",columnDefinition="TEXT") private String sourceCode;
    @Column(name="answered_at",nullable=false) private Instant answeredAt;
    protected ExamAnswerJpaEntity(){}
    ExamAnswerJpaEntity(UUID id,UUID attemptId,UUID questionId,Integer selectedOptionIndex,String sourceCode,Instant answeredAt){this.id=id;this.attemptId=attemptId;this.questionId=questionId;this.selectedOptionIndex=selectedOptionIndex;this.sourceCode=sourceCode;this.answeredAt=answeredAt;}
    UUID getId(){return id;} UUID getAttemptId(){return attemptId;} UUID getQuestionId(){return questionId;} Integer getSelectedOptionIndex(){return selectedOptionIndex;} String getSourceCode(){return sourceCode;} Instant getAnsweredAt(){return answeredAt;}
}
