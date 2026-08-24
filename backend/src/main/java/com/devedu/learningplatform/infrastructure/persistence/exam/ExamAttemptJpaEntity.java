package com.devedu.learningplatform.infrastructure.persistence.exam;

import com.devedu.learningplatform.domain.model.ExamAttemptStatus;
import jakarta.persistence.*;
import java.time.Instant; import java.util.UUID;

@Entity @Table(name="exam_attempts")
class ExamAttemptJpaEntity {
    @Id private UUID id;
    @Column(name="exam_id",nullable=false) private UUID examId;
    @Column(name="student_id",nullable=false) private UUID studentId;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private ExamAttemptStatus status;
    @Column(name="started_at",nullable=false) private Instant startedAt;
    @Column(name="expires_at",nullable=false) private Instant expiresAt;
    @Column(name="submitted_at") private Instant submittedAt;
    @Column(name="automatic_score",nullable=false) private int automaticScore;
    @Column(name="automatic_max_score",nullable=false) private int automaticMaxScore;
    @Column(name="pending_coding_questions",nullable=false) private int pendingCodingQuestions;
    protected ExamAttemptJpaEntity(){}
    ExamAttemptJpaEntity(UUID id,UUID examId,UUID studentId,ExamAttemptStatus status,Instant startedAt,Instant expiresAt,Instant submittedAt,int automaticScore,int automaticMaxScore,int pendingCodingQuestions){this.id=id;this.examId=examId;this.studentId=studentId;this.status=status;this.startedAt=startedAt;this.expiresAt=expiresAt;this.submittedAt=submittedAt;this.automaticScore=automaticScore;this.automaticMaxScore=automaticMaxScore;this.pendingCodingQuestions=pendingCodingQuestions;}
    UUID getId(){return id;} UUID getExamId(){return examId;} UUID getStudentId(){return studentId;} ExamAttemptStatus getStatus(){return status;} Instant getStartedAt(){return startedAt;} Instant getExpiresAt(){return expiresAt;} Instant getSubmittedAt(){return submittedAt;} int getAutomaticScore(){return automaticScore;} int getAutomaticMaxScore(){return automaticMaxScore;} int getPendingCodingQuestions(){return pendingCodingQuestions;}
}
