package com.devedu.learningplatform.infrastructure.persistence.exam;

import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ExamQuestionType;
import jakarta.persistence.*;
import java.time.Instant; import java.util.UUID;

@Entity @Table(name="exam_questions")
class ExamQuestionJpaEntity {
    @Id private UUID id;
    @Column(name="exam_id",nullable=false) private UUID examId;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private ExamQuestionType type;
    @Column(nullable=false,columnDefinition="TEXT") private String prompt;
    @Column(name="correct_option_index") private Integer correctOptionIndex;
    @Enumerated(EnumType.STRING) @Column(name="coding_language",length=20) private CodeLanguage codingLanguage;
    @Column(nullable=false) private int points;
    @Column(nullable=false) private int position;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    protected ExamQuestionJpaEntity(){}
    ExamQuestionJpaEntity(UUID id,UUID examId,ExamQuestionType type,String prompt,Integer correctOptionIndex,CodeLanguage codingLanguage,int points,int position,Instant createdAt){
        this.id=id;this.examId=examId;this.type=type;this.prompt=prompt;this.correctOptionIndex=correctOptionIndex;this.codingLanguage=codingLanguage;this.points=points;this.position=position;this.createdAt=createdAt;
    }
    UUID getId(){return id;} UUID getExamId(){return examId;} ExamQuestionType getType(){return type;} String getPrompt(){return prompt;}
    Integer getCorrectOptionIndex(){return correctOptionIndex;} CodeLanguage getCodingLanguage(){return codingLanguage;} int getPoints(){return points;} int getPosition(){return position;} Instant getCreatedAt(){return createdAt;}
}
