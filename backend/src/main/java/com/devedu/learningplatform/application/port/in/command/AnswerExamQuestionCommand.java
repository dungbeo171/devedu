package com.devedu.learningplatform.application.port.in.command;

import java.util.UUID;

public record AnswerExamQuestionCommand(UUID studentId, UUID attemptId, UUID questionId,
                                        Integer selectedOptionIndex, String sourceCode) {}
