package com.devedu.learningplatform.application.port.in.result;

import com.devedu.learningplatform.domain.model.Exam;
import com.devedu.learningplatform.domain.model.ExamQuestion;

import java.util.List;

public record ExamDetails(Exam exam, List<ExamQuestion> questions) {}
