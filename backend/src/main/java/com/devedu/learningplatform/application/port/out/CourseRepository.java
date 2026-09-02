package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.domain.model.Course;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseRepository {

    boolean existsBySlug(String slug);

    Course save(Course course);

    List<Course> findAll();

    default List<Course> findByTeacherId(UUID teacherId) {
        return findAll().stream().filter(course -> course.teacherId().equals(teacherId)).toList();
    }

    Optional<Course> findById(UUID id);

    Optional<Course> findBySlug(String slug);
}
