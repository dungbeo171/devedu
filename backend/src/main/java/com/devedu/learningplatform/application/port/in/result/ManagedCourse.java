package com.devedu.learningplatform.application.port.in.result;

import com.devedu.learningplatform.domain.model.Course;
import com.devedu.learningplatform.domain.model.CourseStatus;

public record ManagedCourse(Course course, String teacherName, long studentCount, CourseStatus status) {}
