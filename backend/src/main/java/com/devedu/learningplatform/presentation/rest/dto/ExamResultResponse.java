package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.application.port.in.result.ExamResult;
import java.time.Instant; import java.util.List; import java.util.UUID;

public record ExamResultResponse(UUID attemptId,UUID examId,Instant submittedAt,int automaticScore,
                                 int automaticMaxScore,int pendingCodingQuestions,List<ExamAnswerResponse> answers){
    public static ExamResultResponse from(ExamResult result){var a=result.attempt();return new ExamResultResponse(a.id(),a.examId(),a.submittedAt(),a.automaticScore(),a.automaticMaxScore(),a.pendingCodingQuestions(),result.answers().stream().map(ExamAnswerResponse::from).toList());}
}
