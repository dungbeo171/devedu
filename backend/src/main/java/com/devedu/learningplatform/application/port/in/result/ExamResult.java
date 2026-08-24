package com.devedu.learningplatform.application.port.in.result;

import com.devedu.learningplatform.domain.model.ExamAnswer;
import com.devedu.learningplatform.domain.model.ExamAttempt;

import java.util.List;

public record ExamResult(ExamAttempt attempt, List<ExamAnswer> answers) {}
