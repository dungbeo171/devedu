package com.devedu.learningplatform.application.exception;

public class CourseSlugAlreadyExistsException extends RuntimeException {

    public CourseSlugAlreadyExistsException(String slug) {
        super("Course slug is already in use: " + slug);
    }
}

