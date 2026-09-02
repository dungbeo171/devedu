package com.devedu.learningplatform.infrastructure.persistence.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataCourseProblemAssignmentRepository extends JpaRepository<CourseProblemAssignmentJpaEntity, UUID> {
    List<CourseProblemAssignmentJpaEntity> findAllByCourseIdOrderByAssignedAtAsc(UUID courseId);
    boolean existsByCourseIdAndProblemId(UUID courseId, UUID problemId);
    void deleteByCourseIdAndProblemId(UUID courseId, UUID problemId);
}
