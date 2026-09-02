package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.domain.model.CourseEnrollment;
import java.util.List;
import java.util.UUID;

public interface CourseEnrollmentRepository {
    boolean existsByCourseIdAndStudentId(UUID courseId, UUID studentId);
    List<CourseEnrollment> saveAll(List<CourseEnrollment> enrollments);
    List<UUID> findStudentIdsByCourseId(UUID courseId);
    default List<UUID> findCourseIdsByStudentId(UUID studentId) { return List.of(); }
    default List<CourseEnrollment> findAllByCourseId(UUID courseId) {
        return findStudentIdsByCourseId(courseId).stream()
                .map(studentId -> new CourseEnrollment(courseId, studentId, java.time.Instant.EPOCH))
                .toList();
    }
    default void deleteByCourseIdAndStudentIds(UUID courseId, List<UUID> studentIds) {
        throw new UnsupportedOperationException("Removing course students is not implemented");
    }
    default void updateDisplayName(UUID courseId, UUID studentId, String displayName) {
        throw new UnsupportedOperationException("Updating course student is not implemented");
    }
    default long countByCourseId(UUID courseId) { return findStudentIdsByCourseId(courseId).size(); }
}
