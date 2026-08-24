package com.devedu.learningplatform.application.exception;

import java.util.UUID;

public class InterviewQuestionNotFoundException extends RuntimeException {
    public InterviewQuestionNotFoundException(UUID id) { super("Interview question not found: " + id); }
}
