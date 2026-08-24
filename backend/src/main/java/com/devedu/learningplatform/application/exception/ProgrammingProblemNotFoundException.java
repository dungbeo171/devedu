package com.devedu.learningplatform.application.exception;

public class ProgrammingProblemNotFoundException extends RuntimeException {

    public ProgrammingProblemNotFoundException(String slug) {
        super("Programming problem not found: " + slug);
    }
}

