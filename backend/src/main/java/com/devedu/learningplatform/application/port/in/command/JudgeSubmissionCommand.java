package com.devedu.learningplatform.application.port.in.command;

import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ProblemTestCase;
import java.util.List; import java.util.UUID;

public record JudgeSubmissionCommand(UUID submissionId, CodeLanguage language, String sourceCode,
                                     List<ProblemTestCase> testCases) {}
