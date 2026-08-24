package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.application.port.in.result.ExamResult;
import com.devedu.learningplatform.domain.model.ExamAttemptStatus;
import java.time.Instant; import java.util.List; import java.util.UUID;

public record TeacherExamResultResponse(UUID attemptId,UUID studentId,ExamAttemptStatus status,Instant startedAt,
                                        Instant submittedAt,int automaticScore,int automaticMaxScore,int pendingCodingQuestions,
                                        List<ExamAnswerResponse> answers){
    public static TeacherExamResultResponse from(ExamResult result){var a=result.attempt();return new TeacherExamResultResponse(a.id(),a.studentId(),a.status(),a.startedAt(),a.submittedAt(),a.automaticScore(),a.automaticMaxScore(),a.pendingCodingQuestions(),result.answers().stream().map(ExamAnswerResponse::from).toList());}
}
