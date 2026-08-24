package com.devedu.learningplatform.infrastructure.persistence.interview;

import com.devedu.learningplatform.domain.model.InterviewDifficulty;
import com.devedu.learningplatform.domain.model.InterviewTopic;
import jakarta.persistence.*;
import java.time.Instant; import java.util.UUID;

@Entity @Table(name = "interview_questions")
class InterviewQuestionJpaEntity {
    @Id private UUID id;
    @Column(nullable=false,columnDefinition="TEXT") private String question;
    @Column(nullable=false,columnDefinition="TEXT") private String answer;
    @Column(nullable=false,columnDefinition="TEXT") private String explanation;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private InterviewDifficulty difficulty;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private InterviewTopic topic;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    protected InterviewQuestionJpaEntity() {}
    UUID getId(){return id;} String getQuestion(){return question;} String getAnswer(){return answer;} String getExplanation(){return explanation;}
    InterviewDifficulty getDifficulty(){return difficulty;} InterviewTopic getTopic(){return topic;} Instant getCreatedAt(){return createdAt;}
}
