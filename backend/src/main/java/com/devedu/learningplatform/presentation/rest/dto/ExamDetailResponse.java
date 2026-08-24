package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.application.port.in.result.ExamDetails;
import java.time.Instant; import java.util.List; import java.util.UUID;

public record ExamDetailResponse(UUID id,String slug,String title,String description,Instant scheduledAt,
                                 int durationMinutes,List<StudentExamQuestionResponse> questions){
    public static ExamDetailResponse from(ExamDetails details){var e=details.exam();return new ExamDetailResponse(e.id(),e.slug(),e.title(),e.description(),e.scheduledAt(),e.durationMinutes(),details.questions().stream().map(StudentExamQuestionResponse::from).toList());}
}
