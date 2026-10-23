package com.devedu.learningplatform.application.contest;

import com.devedu.learningplatform.domain.contest.ContestProblemDefinition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContestProblemRepository {
    ContestProblemDefinition save(ContestProblemDefinition definition);
    Optional<ContestProblemDefinition> find(UUID contestId, UUID problemId);
    List<ContestProblemDefinition> findAll(UUID contestId);
}
