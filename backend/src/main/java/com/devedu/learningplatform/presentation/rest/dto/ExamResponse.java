package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.Exam;
import java.time.Instant; import java.util.UUID;

public record ExamResponse(UUID id,String slug,String title,String description,Instant scheduledAt,int durationMinutes,Instant createdAt){
    public static ExamResponse from(Exam exam){return new ExamResponse(exam.id(),exam.slug(),exam.title(),exam.description(),exam.scheduledAt(),exam.durationMinutes(),exam.createdAt());}
}
