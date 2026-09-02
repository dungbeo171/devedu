package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ProblemDifficulty;
import com.devedu.learningplatform.domain.model.ProblemTopic;
import com.devedu.learningplatform.domain.model.ProgrammingProblem;

import java.util.List;
import java.util.Optional;
import com.devedu.learningplatform.domain.model.ProblemTestCase;

public interface ProgrammingProblemRepository {

    List<ProgrammingProblem> findAll(ProblemTopic topic, ProblemDifficulty difficulty, CodeLanguage language);

    Optional<ProgrammingProblem> findBySlug(String slug);

    default Optional<ProgrammingProblem> findById(java.util.UUID id) {
        return findAll(null, null, null).stream().filter(problem -> problem.id().equals(id)).findFirst();
    }

    boolean existsBySlug(String slug);

    ProgrammingProblem saveWithTestCases(ProgrammingProblem problem, List<ProblemTestCase> testCases);

    void deleteById(java.util.UUID problemId);
}
