package com.devedu.learningplatform.infrastructure.persistence.problem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface SpringDataProblemSubmissionRepository
        extends JpaRepository<ProblemSubmissionJpaEntity, UUID> {

    @Query("""
            select distinct submission.problemId
            from ProblemSubmissionJpaEntity submission
            where submission.studentId = :studentId
              and submission.status = com.devedu.learningplatform.domain.model.SubmissionStatus.ACCEPTED
            """)
    List<UUID> findAcceptedProblemIdsByStudentId(@Param("studentId") UUID studentId);
}
