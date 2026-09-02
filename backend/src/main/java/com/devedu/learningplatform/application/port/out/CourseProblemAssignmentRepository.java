package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.domain.model.CourseProblemAssignment;

import java.util.List;
import java.util.UUID;

public interface CourseProblemAssignmentRepository {
    List<CourseProblemAssignment> findAllByCourseId(UUID courseId);
    boolean existsByCourseIdAndProblemId(UUID courseId, UUID problemId);
    CourseProblemAssignment save(CourseProblemAssignment assignment);
    void deleteByCourseIdAndProblemId(UUID courseId, UUID problemId);
}
