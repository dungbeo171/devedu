package com.devedu.learningplatform.infrastructure.persistence.course;

import com.devedu.learningplatform.application.exception.CourseSlugAlreadyExistsException;
import com.devedu.learningplatform.application.port.out.CourseRepository;
import com.devedu.learningplatform.domain.model.Course;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CourseRepositoryAdapter implements CourseRepository {
    private final SpringDataCourseRepository repository;

    public CourseRepositoryAdapter(SpringDataCourseRepository repository) {
        this.repository = repository;
    }

    @Override public boolean existsBySlug(String slug) { return repository.existsBySlug(slug); }

    @Override
    public Course save(Course course) {
        try {
            return toDomain(repository.saveAndFlush(new CourseJpaEntity(
                    course.id(), course.slug(), course.title(), course.description(),
                    course.teacherId(), course.startDate(), course.endDate(), course.createdAt()
            )));
        } catch (DataIntegrityViolationException exception) {
            throw new CourseSlugAlreadyExistsException(course.slug());
        }
    }

    @Override public List<Course> findAll() {
        return repository.findAllByOrderByTitleAsc().stream().map(this::toDomain).toList();
    }

    @Override public List<Course> findByTeacherId(UUID teacherId) {
        return repository.findAllByTeacherIdOrderByTitleAsc(teacherId).stream().map(this::toDomain).toList();
    }

    @Override public Optional<Course> findById(UUID id) { return repository.findById(id).map(this::toDomain); }
    @Override public Optional<Course> findBySlug(String slug) { return repository.findBySlug(slug).map(this::toDomain); }

    private Course toDomain(CourseJpaEntity entity) {
        return new Course(entity.getId(), entity.getSlug(), entity.getTitle(), entity.getDescription(),
                entity.getTeacherId(), entity.getStartDate(), entity.getEndDate(), entity.getCreatedAt());
    }
}
