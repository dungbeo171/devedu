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

    ProgrammingProblem saveWithTestCases(ProgrammingProblem problem, List<ProblemTestCase> testCases);
}
