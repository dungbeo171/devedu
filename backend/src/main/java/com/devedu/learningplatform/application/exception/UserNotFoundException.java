package com.devedu.learningplatform.application.exception;

public final class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("User was not found");
    }
}
