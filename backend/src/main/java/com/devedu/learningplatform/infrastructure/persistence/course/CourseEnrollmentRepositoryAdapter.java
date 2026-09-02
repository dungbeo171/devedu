package com.devedu.learningplatform.infrastructure.persistence.course;

import com.devedu.learningplatform.application.port.out.CourseEnrollmentRepository;
import com.devedu.learningplatform.domain.model.CourseEnrollment;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Repository
public class CourseEnrollmentRepositoryAdapter implements CourseEnrollmentRepository {
    private final SpringDataCourseEnrollmentRepository repository;
    public CourseEnrollmentRepositoryAdapter(SpringDataCourseEnrollmentRepository repository) { this.repository = repository; }
    @Override public boolean existsByCourseIdAndStudentId(UUID courseId, UUID studentId) {
        return repository.existsByCourseIdAndStudentId(courseId, studentId);
    }
    @Override @Transactional public List<CourseEnrollment> saveAll(List<CourseEnrollment> enrollments) {
        return repository.saveAll(enrollments.stream().map(item -> new CourseEnrollmentJpaEntity(
                UUID.randomUUID(), item.courseId(), item.studentId(), item.enrolledAt())).toList())
                .stream().map(this::toDomain).toList();
    }
    @Override public List<UUID> findStudentIdsByCourseId(UUID courseId) {
        return repository.findAllByCourseIdOrderByEnrolledAtAsc(courseId).stream()
                .map(CourseEnrollmentJpaEntity::getStudentId).toList();
    }
    @Override public List<CourseEnrollment> findAllByCourseId(UUID courseId) {
        return repository.findAllByCourseIdOrderByEnrolledAtAsc(courseId).stream().map(this::toDomain).toList();
    }
    @Override @Transactional public void deleteByCourseIdAndStudentIds(UUID courseId, List<UUID> studentIds) {
        repository.deleteAllByCourseIdAndStudentIdIn(courseId, studentIds);
    }
    @Override @Transactional public void updateDisplayName(UUID courseId, UUID studentId, String displayName) {
        var enrollment = repository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("Course student was not found"));
        enrollment.setDisplayName(displayName);
        repository.saveAndFlush(enrollment);
    }
    @Override public long countByCourseId(UUID courseId) { return repository.countByCourseId(courseId); }
    @Override public List<UUID> findCourseIdsByStudentId(UUID studentId) {
        return repository.findAllByStudentIdOrderByEnrolledAtDesc(studentId).stream()
                .map(CourseEnrollmentJpaEntity::getCourseId).toList();
    }
    private CourseEnrollment toDomain(CourseEnrollmentJpaEntity entity) {
        return new CourseEnrollment(entity.getCourseId(), entity.getStudentId(), entity.getEnrolledAt(), entity.getDisplayName());
    }
}
