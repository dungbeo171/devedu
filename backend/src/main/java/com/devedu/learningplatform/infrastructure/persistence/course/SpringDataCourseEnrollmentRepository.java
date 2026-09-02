package com.devedu.learningplatform.infrastructure.persistence.course;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

interface SpringDataCourseEnrollmentRepository extends JpaRepository<CourseEnrollmentJpaEntity, UUID> {
    boolean existsByCourseIdAndStudentId(UUID courseId, UUID studentId);
    Optional<CourseEnrollmentJpaEntity> findByCourseIdAndStudentId(UUID courseId, UUID studentId);
    List<CourseEnrollmentJpaEntity> findAllByCourseIdOrderByEnrolledAtAsc(UUID courseId);
    long countByCourseId(UUID courseId);
    List<CourseEnrollmentJpaEntity> findAllByStudentIdOrderByEnrolledAtDesc(UUID studentId);
    void deleteAllByCourseIdAndStudentIdIn(UUID courseId, List<UUID> studentIds);
}
