package com.devedu.learningplatform.application.exception;

public class CourseManagementForbiddenException extends RuntimeException {

    public CourseManagementForbiddenException() {
        super("You do not have permission to manage this course");
    }
}
