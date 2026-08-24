package com.devedu.learningplatform.application.port.in.result;

import com.devedu.learningplatform.domain.model.CourseTopic;
import com.devedu.learningplatform.domain.model.Lesson;

import java.util.List;

public record CourseTopicDetails(CourseTopic topic, List<Lesson> lessons) {
}

