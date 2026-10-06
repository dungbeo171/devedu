package com.devedu.learningplatform.application.contest;

import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.domain.contest.Contest;
import com.devedu.learningplatform.domain.model.*;
import java.time.Instant;
import java.util.*;

public interface ContestUseCase {
    ContestViews.Catalog list(Contest.Status status);
    ContestViews.Catalog list(Contest.Status status, Boolean rated, Contest.Difficulty difficulty, String sort);
    ContestViews.Detail detail(UUID id, AuthenticatedUser viewer);
    UUID create(Create command, AuthenticatedUser creator);
    void register(UUID id, AuthenticatedUser user);
    ProblemSubmission submit(UUID id, UUID problemId, CodeLanguage language, String code, AuthenticatedUser user);
    ProblemSubmission submit(UUID id, UUID problemId, CodeLanguage language, String code, AuthenticatedUser user, String requestKey);
    record Create(String name, Contest.Type type, Instant startsAt, int durationMinutes,
                  String rules, List<Contest.Problem> problems, boolean rated, Contest.Difficulty difficulty) {
        public Create(String name, Contest.Type type, Instant startsAt, int durationMinutes, String rules, List<Contest.Problem> problems) {
            this(name, type, startsAt, durationMinutes, rules, problems, false, Contest.Difficulty.BEGINNER);
        }
    }
}
