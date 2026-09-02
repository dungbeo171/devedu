package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.domain.model.CourseMaterial;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseMaterialRepository {
    CourseMaterial save(CourseMaterial material);
    Optional<CourseMaterial> findById(UUID id);
    List<CourseMaterial> findAllByCourseId(UUID courseId);
}
