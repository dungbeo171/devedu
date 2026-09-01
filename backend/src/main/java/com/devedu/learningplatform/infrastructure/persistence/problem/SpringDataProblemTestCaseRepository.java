package com.devedu.learningplatform.infrastructure.persistence.problem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List; import java.util.UUID;

interface SpringDataProblemTestCaseRepository extends JpaRepository<ProblemTestCaseJpaEntity, UUID> {
    List<ProblemTestCaseJpaEntity> findAllByProblemIdOrderByPositionAsc(UUID problemId);

    @Modifying(flushAutomatically = true)
    @Query("delete from ProblemTestCaseJpaEntity testCase where testCase.problemId = :problemId")
    void deleteAllByProblemId(@Param("problemId") UUID problemId);
}
