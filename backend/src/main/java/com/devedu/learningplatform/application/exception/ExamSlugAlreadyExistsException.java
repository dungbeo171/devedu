package com.devedu.learningplatform.application.exception;

public class ExamSlugAlreadyExistsException extends RuntimeException {
    public ExamSlugAlreadyExistsException(String slug) { super("Exam slug already exists: " + slug); }
}
