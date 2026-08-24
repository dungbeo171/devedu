package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.domain.model.ProblemSubmission;

public interface ProblemSubmissionRepository {

    ProblemSubmission save(ProblemSubmission submission);
}

