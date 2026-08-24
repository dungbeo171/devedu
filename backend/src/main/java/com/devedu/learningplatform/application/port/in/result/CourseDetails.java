package com.devedu.learningplatform.application.port.in.result;

import com.devedu.learningplatform.domain.model.Course;

import java.util.List;

public record CourseDetails(Course course, List<CourseTopicDetails> topics) {
}

