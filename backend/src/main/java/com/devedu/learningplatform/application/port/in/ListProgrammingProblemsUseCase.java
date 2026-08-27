package com.devedu.learningplatform.application.port.in;

import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ProblemDifficulty;
import com.devedu.learningplatform.domain.model.ProblemTopic;
import com.devedu.learningplatform.domain.model.ProgrammingProblem;

import java.util.List;

public interface ListProgrammingProblemsUseCase {

    List<ProgrammingProblem> list(ProblemTopic topic, ProblemDifficulty difficulty, CodeLanguage language);
}
