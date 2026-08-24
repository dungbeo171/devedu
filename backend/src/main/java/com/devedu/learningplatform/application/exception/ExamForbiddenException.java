package com.devedu.learningplatform.application.exception;

public class ExamForbiddenException extends RuntimeException {
    public ExamForbiddenException() { super("You are not allowed to perform this exam operation"); }
}
