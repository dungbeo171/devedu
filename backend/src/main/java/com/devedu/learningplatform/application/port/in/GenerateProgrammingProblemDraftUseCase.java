package com.devedu.learningplatform.application.port.in;

import com.devedu.learningplatform.application.port.in.result.GeneratedProgrammingProblemDraft;

public interface GenerateProgrammingProblemDraftUseCase {

    GeneratedProgrammingProblemDraft generate(String description);
}
