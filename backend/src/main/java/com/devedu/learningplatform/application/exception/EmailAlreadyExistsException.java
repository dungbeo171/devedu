package com.devedu.learningplatform.application.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException() {
        super("Email is already registered");
    }
}

