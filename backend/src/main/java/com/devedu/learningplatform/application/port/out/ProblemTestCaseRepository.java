package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.domain.model.ProblemTestCase;
import java.util.List; import java.util.UUID;

public interface ProblemTestCaseRepository {
    List<ProblemTestCase> findAllByProblemId(UUID problemId);
}
