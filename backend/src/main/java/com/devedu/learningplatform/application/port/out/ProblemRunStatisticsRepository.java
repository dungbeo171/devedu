package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.domain.model.ProblemRunStatistics;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface ProblemRunStatisticsRepository {

    void record(UUID problemId, boolean successful);

    Map<UUID, ProblemRunStatistics> findAllByProblemIds(Set<UUID> problemIds);
}
