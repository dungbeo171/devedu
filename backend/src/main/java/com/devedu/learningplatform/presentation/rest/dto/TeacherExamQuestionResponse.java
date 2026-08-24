package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ExamQuestion;
import com.devedu.learningplatform.domain.model.ExamQuestionType;
import java.util.List; import java.util.UUID;

public record TeacherExamQuestionResponse(UUID id,ExamQuestionType type,String prompt,List<String> options,
                                          Integer correctOptionIndex,CodeLanguage codingLanguage,int points,int position){
    public static TeacherExamQuestionResponse from(ExamQuestion q){return new TeacherExamQuestionResponse(q.id(),q.type(),q.prompt(),q.options(),q.correctOptionIndex(),q.codingLanguage(),q.points(),q.position());}
}
