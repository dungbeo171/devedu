package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.domain.model.CourseTopic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseTopicRepository {

    CourseTopic save(CourseTopic topic);

    Optional<CourseTopic> findById(UUID id);

    List<CourseTopic> findAllByCourseId(UUID courseId);
}

