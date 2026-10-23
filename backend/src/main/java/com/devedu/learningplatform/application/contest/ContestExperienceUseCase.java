package com.devedu.learningplatform.application.contest;

import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.domain.model.*;
import java.util.UUID;
import com.devedu.learningplatform.application.port.in.result.JudgeResult;

public interface ContestExperienceUseCase {
    void finalizeEnded();
    ContestExperienceViews.Result results(UUID id, AuthenticatedUser user);
    ContestExperienceViews.Profile profile(long publicId, AuthenticatedUser viewer);
    ContestExperienceViews.VirtualSession startVirtual(UUID id, AuthenticatedUser user);
    ContestExperienceViews.VirtualSession virtualDetail(UUID id, UUID sessionId, AuthenticatedUser user);
    ProblemSubmission submitVirtual(UUID id, UUID sessionId, UUID problemId, CodeLanguage language, String code, String requestKey, AuthenticatedUser user);
    JudgeResult runVirtual(UUID id, UUID sessionId, UUID problemId, CodeLanguage language, String code, AuthenticatedUser user);
}
