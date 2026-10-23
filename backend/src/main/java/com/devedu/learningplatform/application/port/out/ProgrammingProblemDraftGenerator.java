package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.application.port.in.result.GeneratedProgrammingProblemDraft;

public interface ProgrammingProblemDraftGenerator {

    GeneratedProgrammingProblemDraft generate(String description);
}
