package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.exception.ProgrammingProblemNotFoundException;
import com.devedu.learningplatform.application.exception.ProgrammingProblemSlugAlreadyExistsException;
import com.devedu.learningplatform.application.port.in.ProgrammingProblemsUseCase;
import com.devedu.learningplatform.application.port.in.CodeJudgeUseCase;
import com.devedu.learningplatform.application.port.in.command.JudgeSubmissionCommand;
import com.devedu.learningplatform.application.port.in.command.SubmitProblemCommand;
import com.devedu.learningplatform.application.port.in.command.SaveProblemDraftCommand;
import com.devedu.learningplatform.application.port.in.command.CreateProgrammingProblemCommand;
import com.devedu.learningplatform.application.port.in.command.RunProblemTestsCommand;
import com.devedu.learningplatform.application.port.in.command.UpdateProgrammingProblemCommand;
import com.devedu.learningplatform.application.port.in.command.CreateProblemTestCaseCommand;
import com.devedu.learningplatform.application.port.in.result.ManagedProgrammingProblem;
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
import com.devedu.learningplatform.domain.model.ProblemTestCase;

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
    private static final int MAXIMUM_DESCRIPTION_LENGTH = 50_000;
    private static final int MAXIMUM_TEST_CASES = 50;

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
    public ProgrammingProblem create(CreateProgrammingProblemCommand command) {
        Objects.requireNonNull(command, "Create programming problem command is required");
        var slug = normalizeSlug(command.slug());
        validateProblemFields(slug, command.title(), command.summary(), command.description(),
                command.sampleInput(), command.sampleOutput(), command.topic(), command.difficulty(),
                command.allowedLanguages(), command.starterCodes(), command.testCases());
        if (problemRepository.existsBySlug(slug)) {
            throw new ProgrammingProblemSlugAlreadyExistsException(slug);
        }

        var problemId = UUID.randomUUID();
        var problem = new ProgrammingProblem(
                problemId,
                slug,
                command.title(),
                command.summary(),
                command.description(),
                command.sampleInput(),
                command.sampleOutput(),
                command.topic(),
                command.difficulty(),
                command.allowedLanguages(),
                command.starterCodes(),
                Instant.now(clock)
        );
        var testCases = createTestCases(problemId, command.testCases());
        return problemRepository.saveWithTestCases(problem, testCases);
    }

    @Override
    public ManagedProgrammingProblem getForManagement(String slug) {
        var problem = getBySlug(slug);
        return new ManagedProgrammingProblem(problem, testCaseRepository.findAllByProblemId(problem.id()));
    }

    @Override
    public ProgrammingProblem update(UpdateProgrammingProblemCommand command) {
        Objects.requireNonNull(command, "Update programming problem command is required");
        var existing = getBySlug(command.currentSlug());
        var slug = normalizeSlug(command.slug());
        validateProblemFields(slug, command.title(), command.summary(), command.description(),
                command.sampleInput(), command.sampleOutput(), command.topic(), command.difficulty(),
                command.allowedLanguages(), command.starterCodes(), command.testCases());
        if (!existing.slug().equals(slug) && problemRepository.existsBySlug(slug)) {
            throw new ProgrammingProblemSlugAlreadyExistsException(slug);
        }
        var updated = new ProgrammingProblem(
                existing.id(), slug, command.title(), command.summary(), command.description(),
                command.sampleInput(), command.sampleOutput(), command.topic(), command.difficulty(),
                command.allowedLanguages(), command.starterCodes(), existing.createdAt()
        );
        return problemRepository.saveWithTestCases(updated, createTestCases(existing.id(), command.testCases()));
    }

    @Override
    public void delete(String slug) {
        var problem = getBySlug(slug);
        problemRepository.deleteById(problem.id());
    }

    @Override
    public com.devedu.learningplatform.application.port.in.result.JudgeResult runTests(RunProblemTestsCommand command) {
        Objects.requireNonNull(command, "Run problem tests command is required");
        Objects.requireNonNull(command.language(), "Run language is required");
        requireSourceCode(command.sourceCode());
        var problem = getBySlug(command.problemSlug());
        requireAllowedLanguage(problem, command.language());
        return codeJudge.judge(new JudgeSubmissionCommand(
                UUID.randomUUID(),
                command.language(),
                command.sourceCode(),
                testCaseRepository.findAllByProblemId(problem.id())
        ));
    }

    @Override
    public ProblemSubmission submit(SubmitProblemCommand command) {
        Objects.requireNonNull(command, "Submit problem command is required");
        Objects.requireNonNull(command.studentId(), "Student id is required");
        Objects.requireNonNull(command.language(), "Submission language is required");
        requireSourceCode(command.sourceCode());

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

    private void requireSourceCode(String sourceCode) {
        if (sourceCode == null || sourceCode.isBlank()) {
            throw new IllegalArgumentException("Source code is required");
        }
        if (sourceCode.length() > MAXIMUM_SOURCE_CODE_LENGTH) {
            throw new IllegalArgumentException("Source code must not exceed 100000 characters");
        }
    }

    private void validateProblemFields(
            String slug,
            String title,
            String summary,
            String description,
            String sampleInput,
            String sampleOutput,
            ProblemTopic topic,
            ProblemDifficulty difficulty,
            Set<CodeLanguage> allowedLanguages,
            java.util.Map<CodeLanguage, String> starterCodes,
            List<CreateProblemTestCaseCommand> testCases
    ) {
        if (!slug.matches("[a-z0-9]+(?:-[a-z0-9]+)*") || slug.length() > 120) {
            throw new IllegalArgumentException("Slug must contain at most 120 lowercase letters, numbers and hyphens");
        }
        requireLength(title, "Title", 180);
        requireLength(summary, "Summary", 500);
        requireLength(description, "Description", MAXIMUM_DESCRIPTION_LENGTH);
        requireMaximumLength(sampleInput, "Sample input", MAXIMUM_INPUT_LENGTH);
        requireMaximumLength(sampleOutput, "Sample output", MAXIMUM_INPUT_LENGTH);
        Objects.requireNonNull(topic, "Problem topic is required");
        Objects.requireNonNull(difficulty, "Problem difficulty is required");
        if (allowedLanguages == null || allowedLanguages.isEmpty()) {
            throw new IllegalArgumentException("At least one allowed language is required");
        }
        if (starterCodes == null) {
            throw new IllegalArgumentException("Starter code is required for every allowed language");
        }
        for (var language : allowedLanguages) {
            var starterCode = starterCodes.get(language);
            if (starterCode == null || starterCode.isBlank()) {
                throw new IllegalArgumentException("Starter code is required for " + language);
            }
            if (starterCode.length() > MAXIMUM_SOURCE_CODE_LENGTH) {
                throw new IllegalArgumentException("Starter code must not exceed 100000 characters");
            }
        }
        if (testCases == null || testCases.isEmpty()) {
            throw new IllegalArgumentException("At least one test case is required");
        }
        if (testCases.size() > MAXIMUM_TEST_CASES) {
            throw new IllegalArgumentException("Test cases must not exceed 50");
        }
    }

    private List<ProblemTestCase> createTestCases(UUID problemId, List<CreateProblemTestCaseCommand> requestedTestCases) {
        return java.util.stream.IntStream.range(0, requestedTestCases.size())
                .mapToObj(index -> {
                    var requested = Objects.requireNonNull(requestedTestCases.get(index), "Test case is required");
                    requireMaximumLength(requested.input(), "Test case input", MAXIMUM_INPUT_LENGTH);
                    requireMaximumLength(requested.expectedOutput(), "Expected output", MAXIMUM_INPUT_LENGTH);
                    return new ProblemTestCase(
                            UUID.randomUUID(), problemId, requested.input(), requested.expectedOutput(),
                            requested.timeLimitMillis(), index + 1
                    );
                })
                .toList();
    }

    private String normalizeSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Problem slug is required");
        }
        return slug.trim().toLowerCase(Locale.ROOT);
    }

    private void requireLength(String value, String field, int maximumLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        requireMaximumLength(value, field, maximumLength);
    }

    private void requireMaximumLength(String value, String field, int maximumLength) {
        if (value != null && value.length() > maximumLength) {
            throw new IllegalArgumentException(field + " must not exceed " + maximumLength + " characters");
        }
    }
}
