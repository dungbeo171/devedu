package com.devedu.learningplatform.application.exception;

public final class UserDeletionConflictException extends RuntimeException {

    public UserDeletionConflictException() {
        super("User cannot be deleted because the account has related learning or teaching data");
    }
}
