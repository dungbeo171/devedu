package com.devedu.learningplatform.application.contest;

import com.devedu.learningplatform.application.port.in.command.SubmitProblemCommand;
import com.devedu.learningplatform.domain.model.ProblemSubmission;
import java.time.Instant;
import java.util.UUID;

public interface ContestSubmissionPort {
    // Persist the existing ProblemSubmission and contest association in one transaction.
    ProblemSubmission submit(UUID contestId, SubmitProblemCommand command, Instant receivedAt);
    default ProblemSubmission submit(UUID contestId, SubmitProblemCommand command, Instant receivedAt, UUID virtualId, String requestKey) {
        return submit(contestId, command, receivedAt);
    }
}
