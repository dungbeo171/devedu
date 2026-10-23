package com.devedu.learningplatform.application.contest;

import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.domain.contest.Contest;
import com.devedu.learningplatform.domain.model.*;
import java.time.Instant;
import java.util.*;
import com.devedu.learningplatform.application.port.in.command.CreateProgrammingProblemCommand;
import com.devedu.learningplatform.application.port.in.result.JudgeResult;

public interface ContestUseCase {
    ContestViews.Catalog list(Contest.Status status);
    ContestViews.Catalog list(Contest.Status status, Contest.Difficulty difficulty, String sort);
    ContestViews.ManagedCatalog managed(AuthenticatedUser creator);
    ContestViews.ManagedCatalog supervised(AuthenticatedUser supervisor);
    ContestViews.Detail detail(UUID id, AuthenticatedUser viewer);
    UUID create(Create command, AuthenticatedUser creator);
    ContestViews.Manage manage(UUID id, AuthenticatedUser editor);
    List<ContestViews.UserOption> searchUsers(UserRole role, String query, AuthenticatedUser editor);
    void updateDetails(UUID id, UpdateDetails command, AuthenticatedUser editor);
    void updateProblems(UUID id, List<Contest.Problem> problems, AuthenticatedUser editor);
    void updateSettings(UUID id, UpdateSettings command, AuthenticatedUser editor);
    void publish(UUID id, AuthenticatedUser editor);
    ProgrammingProblem createLocalProblem(UUID id, CreateProgrammingProblemCommand command, AuthenticatedUser editor);
    ProgrammingProblem problem(UUID id, UUID problemId, AuthenticatedUser viewer);
    JudgeResult run(UUID id, UUID problemId, CodeLanguage language, String code, AuthenticatedUser user);
    void delete(UUID id, AuthenticatedUser editor);
    void register(UUID id, AuthenticatedUser user);
    com.devedu.learningplatform.domain.contest.ContestAttempt startAttempt(UUID id, AuthenticatedUser user);
    ProblemSubmission submit(UUID id, UUID problemId, CodeLanguage language, String code, AuthenticatedUser user);
    ProblemSubmission submit(UUID id, UUID problemId, CodeLanguage language, String code, AuthenticatedUser user, String requestKey);
    record Create(String name, Contest.Type type, Instant startsAt, int durationMinutes,
                  List<Contest.Problem> problems, Contest.Difficulty difficulty,
                  Instant endsAt, String description, boolean monitoringEnabled) {
        public Create(String name, Contest.Type type, Instant startsAt, int durationMinutes,
                      List<Contest.Problem> problems, Contest.Difficulty difficulty) {
            this(name, type, startsAt, durationMinutes, problems, difficulty, null, "", false);
        }
        public Create(String name, Contest.Type type, Instant startsAt, int durationMinutes, List<Contest.Problem> problems) {
            this(name, type, startsAt, durationMinutes, problems, Contest.Difficulty.BEGINNER);
        }
    }
    record UpdateDetails(String name, Contest.Type type, Instant startsAt, Instant endsAt,
                         int durationMinutes, Contest.Difficulty difficulty, String description,
                         boolean monitoringEnabled) {}
    record UpdateSettings(Contest.Visibility visibility, Contest.ParticipationMode participationMode,
                          List<String> allowedEmails, List<Long> proctorPublicIds) {}
}
