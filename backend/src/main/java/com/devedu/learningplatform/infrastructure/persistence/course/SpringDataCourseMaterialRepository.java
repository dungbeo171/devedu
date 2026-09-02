package com.devedu.learningplatform.infrastructure.persistence.course;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

interface SpringDataCourseMaterialRepository extends JpaRepository<CourseMaterialJpaEntity, UUID> {
    List<CourseMaterialJpaEntity> findAllByCourseIdOrderByUploadedAtDesc(UUID courseId);
}
