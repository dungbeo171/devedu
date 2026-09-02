package com.devedu.learningplatform.infrastructure.persistence.course;

import com.devedu.learningplatform.application.port.out.CourseProblemAssignmentRepository;
import com.devedu.learningplatform.domain.model.CourseProblemAssignment;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public class CourseProblemAssignmentRepositoryAdapter implements CourseProblemAssignmentRepository {
    private final SpringDataCourseProblemAssignmentRepository repository;
    public CourseProblemAssignmentRepositoryAdapter(SpringDataCourseProblemAssignmentRepository repository) { this.repository = repository; }
    @Override public List<CourseProblemAssignment> findAllByCourseId(UUID courseId) {
        return repository.findAllByCourseIdOrderByAssignedAtAsc(courseId).stream().map(this::toDomain).toList();
    }
    @Override public boolean existsByCourseIdAndProblemId(UUID courseId, UUID problemId) { return repository.existsByCourseIdAndProblemId(courseId, problemId); }
    @Override public CourseProblemAssignment save(CourseProblemAssignment assignment) {
        return toDomain(repository.saveAndFlush(new CourseProblemAssignmentJpaEntity(UUID.randomUUID(), assignment.courseId(), assignment.problemId(), assignment.assignedAt())));
    }
    @Override @Transactional public void deleteByCourseIdAndProblemId(UUID courseId, UUID problemId) { repository.deleteByCourseIdAndProblemId(courseId, problemId); }
    private CourseProblemAssignment toDomain(CourseProblemAssignmentJpaEntity entity) { return new CourseProblemAssignment(entity.getCourseId(), entity.getProblemId(), entity.getAssignedAt()); }
}
