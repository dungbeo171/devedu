package com.devedu.learningplatform.application.port.in;

import com.devedu.learningplatform.application.port.in.command.SaveProblemDraftCommand;
import com.devedu.learningplatform.domain.model.ProblemDraft;

public interface SaveProblemDraftUseCase {

    ProblemDraft saveDraft(SaveProblemDraftCommand command);
}
