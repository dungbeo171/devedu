package com.devedu.learningplatform.infrastructure.persistence.problem;

import com.devedu.learningplatform.application.port.out.ProblemRunStatisticsRepository;
import com.devedu.learningplatform.domain.model.ProblemRunStatistics;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Repository
public class ProblemRunStatisticsRepositoryAdapter implements ProblemRunStatisticsRepository {

    private final SpringDataProblemRunStatisticsRepository repository;

    public ProblemRunStatisticsRepositoryAdapter(SpringDataProblemRunStatisticsRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void record(UUID problemId, boolean successful) {
        repository.record(problemId, successful ? 1 : 0);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, ProblemRunStatistics> findAllByProblemIds(Set<UUID> problemIds) {
        var result = new LinkedHashMap<UUID, ProblemRunStatistics>();
        if (problemIds.isEmpty()) return result;
        repository.findAllById(problemIds).forEach(entity -> result.put(
                entity.getProblemId(),
                new ProblemRunStatistics(entity.getProblemId(), entity.getTotalRuns(), entity.getSuccessfulRuns())
        ));
        return result;
    }
}
