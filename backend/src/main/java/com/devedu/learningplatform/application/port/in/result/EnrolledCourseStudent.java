package com.devedu.learningplatform.application.port.in.result;

import com.devedu.learningplatform.domain.model.User;

import java.time.Instant;

public record EnrolledCourseStudent(User user, Instant enrolledAt, String displayName) {}
