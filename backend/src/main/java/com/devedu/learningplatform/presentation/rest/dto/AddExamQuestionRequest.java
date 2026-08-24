package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ExamQuestionType;
import java.util.List;

public record AddExamQuestionRequest(ExamQuestionType type, String prompt, List<String> options,
                                     Integer correctOptionIndex, CodeLanguage codingLanguage,
                                     int points, int position) {}
