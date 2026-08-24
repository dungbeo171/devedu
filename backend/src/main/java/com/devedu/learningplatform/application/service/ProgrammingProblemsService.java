package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.exception.ProgrammingProblemNotFoundException;
import com.devedu.learningplatform.application.port.in.ProgrammingProblemsUseCase;
import com.devedu.learningplatform.application.port.in.CodeJudgeUseCase;
import com.devedu.learningplatform.application.port.in.command.JudgeSubmissionCommand;
import com.devedu.learningplatform.application.port.in.command.SubmitProblemCommand;
import com.devedu.learningplatform.application.port.out.ProblemSubmissionRepository;
import com.devedu.learningplatform.application.port.out.ProgrammingProblemRepository;
import com.devedu.learningplatform.application.port.out.ProblemTestCaseRepository;
import com.devedu.learningplatform.domain.model.ProblemSubmission;
import com.devedu.learningplatform.domain.model.ProblemTopic;
import com.devedu.learningplatform.domain.model.ProgrammingProblem;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class ProgrammingProblemsService implements ProgrammingProblemsUseCase {

    private static final int MAXIMUM_SOURCE_CODE_LENGTH = 100_000;

    private final ProgrammingProblemRepository problemRepository;
    private final ProblemSubmissionRepository submissionRepository;
    private final ProblemTestCaseRepository testCaseRepository;
    private final CodeJudgeUseCase codeJudge;
    private final Clock clock;

    public ProgrammingProblemsService(
            ProgrammingProblemRepository problemRepository,
            ProblemSubmissionRepository submissionRepository,
            ProblemTestCaseRepository testCaseRepository,
            CodeJudgeUseCase codeJudge,
            Clock clock
    ) {
        this.problemRepository = problemRepository;
        this.submissionRepository = submissionRepository;
        this.testCaseRepository = testCaseRepository;
        this.codeJudge = codeJudge;
        this.clock = clock;
    }

    @Override
    public List<ProgrammingProblem> list(ProblemTopic topic) {
        return problemRepository.findAll(topic);
    }

    @Override
    public ProgrammingProblem getBySlug(String slug) {
        var normalizedSlug = normalizeSlug(slug);
        return problemRepository.findBySlug(normalizedSlug)
                .orElseThrow(() -> new ProgrammingProblemNotFoundException(normalizedSlug));
    }

    @Override
    public ProblemSubmission submit(SubmitProblemCommand command) {
        Objects.requireNonNull(command, "Submit problem command is required");
        Objects.requireNonNull(command.studentId(), "Student id is required");
        Objects.requireNonNull(command.language(), "Submission language is required");
        if (command.sourceCode() == null || command.sourceCode().isBlank()) {
            throw new IllegalArgumentException("Source code is required");
        }
        if (command.sourceCode().length() > MAXIMUM_SOURCE_CODE_LENGTH) {
            throw new IllegalArgumentException("Source code must not exceed 100000 characters");
        }

        var problem = getBySlug(command.problemSlug());
        var submissionId = UUID.randomUUID();
        var judgeResult = codeJudge.judge(new JudgeSubmissionCommand(
                submissionId,
                command.language(),
                command.sourceCode(),
                testCaseRepository.findAllByProblemId(problem.id())
        ));
        var submission = new ProblemSubmission(
                submissionId,
                problem.id(),
                command.studentId(),
                command.language(),
                command.sourceCode(),
                judgeResult.status(),
                judgeResult.diagnostic(),
                judgeResult.passedTests(),
                judgeResult.totalTests(),
                judgeResult.executionTimeMillis(),
                Instant.now(clock)
        );
        return submissionRepository.save(submission);
    }

    private String normalizeSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Problem slug is required");
        }
        return slug.trim().toLowerCase(Locale.ROOT);
    }
}
