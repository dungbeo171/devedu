package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.domain.model.Lesson;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonRepository {

    Lesson save(Lesson lesson);

    Optional<Lesson> findById(UUID id);

    List<Lesson> findAllByTopicIds(List<UUID> topicIds);
}

