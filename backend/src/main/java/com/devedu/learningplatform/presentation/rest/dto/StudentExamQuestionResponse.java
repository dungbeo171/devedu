package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ExamQuestion;
import com.devedu.learningplatform.domain.model.ExamQuestionType;
import java.util.List; import java.util.UUID;

public record StudentExamQuestionResponse(UUID id,ExamQuestionType type,String prompt,List<String> options,
                                          CodeLanguage codingLanguage,int points,int position){
    public static StudentExamQuestionResponse from(ExamQuestion q){return new StudentExamQuestionResponse(q.id(),q.type(),q.prompt(),q.options(),q.codingLanguage(),q.points(),q.position());}
}
