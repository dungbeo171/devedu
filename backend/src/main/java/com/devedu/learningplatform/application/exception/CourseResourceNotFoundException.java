package com.devedu.learningplatform.application.exception;

public class CourseResourceNotFoundException extends RuntimeException {

    public CourseResourceNotFoundException(String resource, Object identifier) {
        super(resource + " not found: " + identifier);
    }
}

