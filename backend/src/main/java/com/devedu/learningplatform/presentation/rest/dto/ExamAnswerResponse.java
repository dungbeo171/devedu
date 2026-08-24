package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.ExamAnswer;
import java.time.Instant; import java.util.UUID;

public record ExamAnswerResponse(UUID id,UUID questionId,Integer selectedOptionIndex,String sourceCode,Instant answeredAt){
    public static ExamAnswerResponse from(ExamAnswer a){return new ExamAnswerResponse(a.id(),a.questionId(),a.selectedOptionIndex(),a.sourceCode(),a.answeredAt());}
}
