package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.application.port.in.result.ExamAttemptSession;
import com.devedu.learningplatform.domain.model.ExamAttemptStatus;
import java.time.Instant; import java.util.List; import java.util.UUID;

public record ExamAttemptSessionResponse(UUID attemptId,ExamAttemptStatus status,Instant startedAt,Instant expiresAt,
                                         ExamDetailResponse exam,List<ExamAnswerResponse> answers){
    public static ExamAttemptSessionResponse from(ExamAttemptSession session){var a=session.attempt();return new ExamAttemptSessionResponse(a.id(),a.status(),a.startedAt(),a.expiresAt(),ExamDetailResponse.from(session.examDetails()),session.answers().stream().map(ExamAnswerResponse::from).toList());}
}
