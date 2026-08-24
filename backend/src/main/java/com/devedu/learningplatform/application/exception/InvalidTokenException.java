package com.devedu.learningplatform.application.exception;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException() {
        super("Token is invalid or expired");
    }
}

