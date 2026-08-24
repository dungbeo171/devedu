package com.devedu.learningplatform.application.port.in.command;

import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ExamQuestionType;
import com.devedu.learningplatform.domain.model.UserRole;

import java.util.List;
import java.util.UUID;

public record AddExamQuestionCommand(UUID actorId, UserRole actorRole, UUID examId, ExamQuestionType type,
                                     String prompt, List<String> options, Integer correctOptionIndex,
                                     CodeLanguage codingLanguage, int points, int position) {}
