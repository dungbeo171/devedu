package com.devedu.learningplatform.infrastructure.persistence.exam;

import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name="exam_question_options")
class ExamQuestionOptionJpaEntity {
    @Id private UUID id;
    @Column(name="question_id",nullable=false) private UUID questionId;
    @Column(name="option_index",nullable=false) private int optionIndex;
    @Column(nullable=false,length=1000) private String value;
    protected ExamQuestionOptionJpaEntity(){}
    ExamQuestionOptionJpaEntity(UUID id,UUID questionId,int optionIndex,String value){this.id=id;this.questionId=questionId;this.optionIndex=optionIndex;this.value=value;}
    UUID getQuestionId(){return questionId;} int getOptionIndex(){return optionIndex;} String getValue(){return value;}
}
