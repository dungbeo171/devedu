package com.devedu.learningplatform.application.exception;

public class JudgeUnavailableException extends RuntimeException {
    public JudgeUnavailableException(String message) { super(message); }
    public JudgeUnavailableException(String message, Throwable cause) { super(message, cause); }
}
