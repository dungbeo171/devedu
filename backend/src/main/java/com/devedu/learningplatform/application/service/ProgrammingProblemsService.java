package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.exception.ProgrammingProblemNotFoundException;
import com.devedu.learningplatform.application.port.in.ProgrammingProblemsUseCase;
import com.devedu.learningplatform.application.port.in.CodeJudgeUseCase;
import com.devedu.learningplatform.application.port.in.command.JudgeSubmissionCommand;
import com.devedu.learningplatform.application.port.in.command.SubmitProblemCommand;
import com.devedu.learningplatform.application.port.in.command.SaveProblemDraftCommand;
import com.devedu.learningplatform.application.port.out.ProblemDraftRepository;
import com.devedu.learningplatform.application.port.out.ProblemSubmissionRepository;
import com.devedu.learningplatform.application.port.out.ProgrammingProblemRepository;
import com.devedu.learningplatform.application.port.out.ProblemTestCaseRepository;
import com.devedu.learningplatform.domain.model.ProblemSubmission;
import com.devedu.learningplatform.domain.model.ProblemDraft;
import com.devedu.learningplatform.domain.model.ProblemTopic;
import com.devedu.learningplatform.domain.model.ProblemDifficulty;
import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ProgrammingProblem;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;

public final class ProgrammingProblemsService implements ProgrammingProblemsUseCase {

    private static final int MAXIMUM_SOURCE_CODE_LENGTH = 100_000;
    private static final int MAXIMUM_INPUT_LENGTH = 100_000;

    private final ProgrammingProblemRepository problemRepository;
    private final ProblemSubmissionRepository submissionRepository;
    private final ProblemDraftRepository draftRepository;
    private final ProblemTestCaseRepository testCaseRepository;
    private final CodeJudgeUseCase codeJudge;
    private final Clock clock;

    public ProgrammingProblemsService(
            ProgrammingProblemRepository problemRepository,
            ProblemSubmissionRepository submissionRepository,
            ProblemDraftRepository draftRepository,
            ProblemTestCaseRepository testCaseRepository,
            CodeJudgeUseCase codeJudge,
            Clock clock
    ) {
        this.problemRepository = problemRepository;
        this.submissionRepository = submissionRepository;
        this.draftRepository = draftRepository;
        this.testCaseRepository = testCaseRepository;
        this.codeJudge = codeJudge;
        this.clock = clock;
    }

    @Override
    public List<ProgrammingProblem> list(ProblemTopic topic, ProblemDifficulty difficulty, CodeLanguage language) {
        return problemRepository.findAll(topic, difficulty, language);
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
        requireAllowedLanguage(problem, command.language());
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
        var savedSubmission = submissionRepository.save(submission);
        var existingInput = draftRepository.findByStudentIdAndProblemId(command.studentId(), problem.id())
                .map(ProblemDraft::input)
                .orElse("");
        draftRepository.save(new ProblemDraft(
                problem.id(),
                command.studentId(),
                command.language(),
                command.sourceCode(),
                existingInput,
                Instant.now(clock)
        ));
        return savedSubmission;
    }

    @Override
    public Set<UUID> listSolvedProblemIds(UUID studentId) {
        Objects.requireNonNull(studentId, "Student id is required");
        return submissionRepository.findAcceptedProblemIdsByStudentId(studentId);
    }

    @Override
    public Optional<ProblemDraft> getDraft(UUID studentId, String problemSlug) {
        Objects.requireNonNull(studentId, "Student id is required");
        var problem = getBySlug(problemSlug);
        return draftRepository.findByStudentIdAndProblemId(studentId, problem.id());
    }

    @Override
    public ProblemDraft saveDraft(SaveProblemDraftCommand command) {
        Objects.requireNonNull(command, "Save problem draft command is required");
        Objects.requireNonNull(command.studentId(), "Student id is required");
        Objects.requireNonNull(command.language(), "Draft language is required");
        var sourceCode = command.sourceCode() == null ? "" : command.sourceCode();
        var input = command.input() == null ? "" : command.input();
        if (sourceCode.length() > MAXIMUM_SOURCE_CODE_LENGTH) {
            throw new IllegalArgumentException("Source code must not exceed 100000 characters");
        }
        if (input.length() > MAXIMUM_INPUT_LENGTH) {
            throw new IllegalArgumentException("Input must not exceed 100000 characters");
        }
        var problem = getBySlug(command.problemSlug());
        requireAllowedLanguage(problem, command.language());
        return draftRepository.save(new ProblemDraft(
                problem.id(),
                command.studentId(),
                command.language(),
                sourceCode,
                input,
                Instant.now(clock)
        ));
    }

    private void requireAllowedLanguage(ProgrammingProblem problem, CodeLanguage language) {
        if (!problem.allowedLanguages().contains(language)) {
            throw new IllegalArgumentException("Language " + language + " is not allowed for this problem");
        }
    }

    private String normalizeSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Problem slug is required");
        }
        return slug.trim().toLowerCase(Locale.ROOT);
    }
}
