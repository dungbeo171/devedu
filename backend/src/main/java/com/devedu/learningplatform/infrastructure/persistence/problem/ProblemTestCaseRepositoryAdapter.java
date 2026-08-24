package com.devedu.learningplatform.infrastructure.persistence.problem;

import com.devedu.learningplatform.application.port.out.ProblemTestCaseRepository;
import com.devedu.learningplatform.domain.model.ProblemTestCase;
import org.springframework.stereotype.Repository;
import java.util.List; import java.util.UUID;

@Repository
public class ProblemTestCaseRepositoryAdapter implements ProblemTestCaseRepository {
    private final SpringDataProblemTestCaseRepository repository;
    public ProblemTestCaseRepositoryAdapter(SpringDataProblemTestCaseRepository repository){this.repository=repository;}
    @Override public List<ProblemTestCase> findAllByProblemId(UUID problemId){return repository.findAllByProblemIdOrderByPositionAsc(problemId).stream().map(e->new ProblemTestCase(e.getId(),e.getProblemId(),e.getInput(),e.getExpectedOutput(),e.getTimeLimitMillis(),e.getPosition())).toList();}
}
