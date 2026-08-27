package com.devedu.learningplatform.application.exception;

public final class ProgrammingProblemSlugAlreadyExistsException extends RuntimeException {
    public ProgrammingProblemSlugAlreadyExistsException(String slug) {
        super("Programming problem slug already exists: " + slug);
    }
}
