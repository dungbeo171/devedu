package com.devedu.learningplatform.application.exception;

public class ExamResourceNotFoundException extends RuntimeException {
    public ExamResourceNotFoundException(String resource, Object identifier) { super(resource + " not found: " + identifier); }
}
