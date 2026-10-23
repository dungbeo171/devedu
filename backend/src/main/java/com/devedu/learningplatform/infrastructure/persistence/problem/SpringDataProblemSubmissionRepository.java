package com.devedu.learningplatform.infrastructure.persistence.problem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.time.Instant;
import com.devedu.learningplatform.domain.model.CodeLanguage;

interface SpringDataProblemSubmissionRepository
        extends JpaRepository<ProblemSubmissionJpaEntity, UUID> {

    @Query("""
            select distinct submission.problemId
            from ProblemSubmissionJpaEntity submission
            where submission.studentId = :studentId
              and submission.status = com.devedu.learningplatform.domain.model.SubmissionStatus.ACCEPTED
            """)
    List<UUID> findAcceptedProblemIdsByStudentId(@Param("studentId") UUID studentId);

    interface AcceptedProblemPair {
        UUID getStudentId();
        UUID getProblemId();
    }

    @Query("""
            select distinct submission.studentId as studentId, submission.problemId as problemId
            from ProblemSubmissionJpaEntity submission
            where submission.studentId in :studentIds
              and submission.status = com.devedu.learningplatform.domain.model.SubmissionStatus.ACCEPTED
            """)
    List<AcceptedProblemPair> findAcceptedProblemIdsByStudentIds(@Param("studentIds") Set<UUID> studentIds);

    interface RecommendationActivityProjection {
        UUID getProblemId();
        CodeLanguage getLanguage();
        long getAttempts();
        long getAcceptedAttempts();
        Instant getLastSubmittedAt();
    }

    @Query("""
            select submission.problemId as problemId,
                   submission.language as language,
                   count(submission.id) as attempts,
                   sum(case when submission.status = com.devedu.learningplatform.domain.model.SubmissionStatus.ACCEPTED then 1 else 0 end) as acceptedAttempts,
                   max(submission.submittedAt) as lastSubmittedAt
            from ProblemSubmissionJpaEntity submission
            where submission.studentId = :studentId
            group by submission.problemId, submission.language
            """)
    List<RecommendationActivityProjection> findRecommendationActivityByStudentId(
            @Param("studentId") UUID studentId
    );
}
