package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.domain.model.ProblemTopic;
import com.devedu.learningplatform.domain.model.ProgrammingProblem;

import java.util.List;
import java.util.Optional;

public interface ProgrammingProblemRepository {

    List<ProgrammingProblem> findAll(ProblemTopic topic);

    Optional<ProgrammingProblem> findBySlug(String slug);
}

