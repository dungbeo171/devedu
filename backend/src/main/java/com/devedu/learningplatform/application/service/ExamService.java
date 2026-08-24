package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.exception.ExamForbiddenException;
import com.devedu.learningplatform.application.exception.ExamResourceNotFoundException;
import com.devedu.learningplatform.application.exception.ExamSlugAlreadyExistsException;
import com.devedu.learningplatform.application.exception.ExamStateException;
import com.devedu.learningplatform.application.port.in.ExamUseCase;
import com.devedu.learningplatform.application.port.in.command.AddExamQuestionCommand;
import com.devedu.learningplatform.application.port.in.command.AnswerExamQuestionCommand;
import com.devedu.learningplatform.application.port.in.command.CreateExamCommand;
import com.devedu.learningplatform.application.port.in.result.ExamAttemptSession;
import com.devedu.learningplatform.application.port.in.result.ExamDetails;
import com.devedu.learningplatform.application.port.in.result.ExamResult;
import com.devedu.learningplatform.application.port.out.ExamAnswerRepository;
import com.devedu.learningplatform.application.port.out.ExamAttemptRepository;
import com.devedu.learningplatform.application.port.out.ExamQuestionRepository;
import com.devedu.learningplatform.application.port.out.ExamRepository;
import com.devedu.learningplatform.domain.model.Exam;
import com.devedu.learningplatform.domain.model.ExamAnswer;
import com.devedu.learningplatform.domain.model.ExamAttempt;
import com.devedu.learningplatform.domain.model.ExamAttemptStatus;
import com.devedu.learningplatform.domain.model.ExamQuestion;
import com.devedu.learningplatform.domain.model.ExamQuestionType;
import com.devedu.learningplatform.domain.model.UserRole;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ExamService implements ExamUseCase {
    private static final int MAX_SOURCE_CODE_LENGTH = 100_000;
    private final ExamRepository examRepository;
    private final ExamQuestionRepository questionRepository;
    private final ExamAttemptRepository attemptRepository;
    private final ExamAnswerRepository answerRepository;
    private final Clock clock;

    public ExamService(ExamRepository examRepository, ExamQuestionRepository questionRepository,
                       ExamAttemptRepository attemptRepository, ExamAnswerRepository answerRepository, Clock clock) {
        this.examRepository = examRepository; this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository; this.answerRepository = answerRepository; this.clock = clock;
    }

    @Override
    public Exam createExam(CreateExamCommand command) {
        Objects.requireNonNull(command, "Create exam command is required");
        requireTeacher(command.actorRole());
        Objects.requireNonNull(command.actorId(), "Actor id is required");
        var slug = Exam.normalizeSlug(command.slug());
        if (examRepository.existsBySlug(slug)) throw new ExamSlugAlreadyExistsException(slug);
        return examRepository.save(new Exam(UUID.randomUUID(), slug, command.title(), command.description(),
                command.actorId(), command.scheduledAt(), command.durationMinutes(), Instant.now(clock)));
    }

    @Override
    public ExamQuestion addQuestion(AddExamQuestionCommand command) {
        Objects.requireNonNull(command, "Add question command is required");
        var exam = getExam(command.examId());
        requireManager(exam, command.actorId(), command.actorRole());
        if (attemptRepository.existsByExamId(exam.id())) throw new ExamStateException("Questions cannot be changed after an attempt has started");
        return questionRepository.save(new ExamQuestion(UUID.randomUUID(), exam.id(), command.type(), command.prompt(),
                command.options(), command.correctOptionIndex(), command.codingLanguage(), command.points(),
                command.position(), Instant.now(clock)));
    }

    @Override public List<Exam> listExams() { return examRepository.findAll(); }

    @Override
    public List<Exam> listManagedExams(UUID actorId, UserRole actorRole) {
        requireTeacher(actorRole);
        Objects.requireNonNull(actorId, "Actor id is required");
        return actorRole == UserRole.ADMIN
                ? examRepository.findAll()
                : examRepository.findAllByTeacherId(actorId);
    }

    @Override
    public ExamDetails getExamBySlug(String slug) {
        var exam = examRepository.findBySlug(Exam.normalizeSlug(slug))
                .orElseThrow(() -> new ExamResourceNotFoundException("Exam", slug));
        return details(exam);
    }

    @Override
    public ExamAttemptSession startExam(UUID studentId, String slug) {
        Objects.requireNonNull(studentId, "Student id is required");
        var details = getExamBySlug(slug);
        if (details.questions().isEmpty()) throw new ExamStateException("Exam has no questions");
        var now = Instant.now(clock);
        if (now.isBefore(details.exam().scheduledAt())) throw new ExamStateException("Exam has not started yet");
        var attempt = attemptRepository.findByExamIdAndStudentId(details.exam().id(), studentId)
                .orElseGet(() -> attemptRepository.save(new ExamAttempt(UUID.randomUUID(), details.exam().id(), studentId,
                        ExamAttemptStatus.IN_PROGRESS, now, now.plus(details.exam().durationMinutes(), ChronoUnit.MINUTES),
                        null, 0, 0, 0)));
        return new ExamAttemptSession(details, attempt, answerRepository.findAllByAttemptId(attempt.id()));
    }

    @Override
    public ExamAttemptSession getAttempt(UUID studentId, UUID attemptId) {
        var attempt = ownedAttempt(studentId, attemptId);
        return new ExamAttemptSession(details(getExam(attempt.examId())), attempt,
                answerRepository.findAllByAttemptId(attempt.id()));
    }

    @Override
    public ExamAnswer answerQuestion(AnswerExamQuestionCommand command) {
        Objects.requireNonNull(command, "Answer command is required");
        var attempt = ownedAttempt(command.studentId(), command.attemptId());
        if (attempt.status() != ExamAttemptStatus.IN_PROGRESS) throw new ExamStateException("Exam attempt has already been submitted");
        if (Instant.now(clock).isAfter(attempt.expiresAt())) throw new ExamStateException("Exam attempt has expired");
        var question = questionRepository.findById(command.questionId())
                .orElseThrow(() -> new ExamResourceNotFoundException("Exam question", command.questionId()));
        if (!question.examId().equals(attempt.examId())) throw new ExamStateException("Question does not belong to this exam");
        validateAnswer(question, command.selectedOptionIndex(), command.sourceCode());
        var existing = answerRepository.findByAttemptIdAndQuestionId(attempt.id(), question.id());
        return answerRepository.save(new ExamAnswer(existing.map(ExamAnswer::id).orElseGet(UUID::randomUUID),
                attempt.id(), question.id(), command.selectedOptionIndex(), command.sourceCode(), Instant.now(clock)));
    }

    @Override
    public ExamResult submitExam(UUID studentId, UUID attemptId) {
        var attempt = ownedAttempt(studentId, attemptId);
        if (attempt.status() == ExamAttemptStatus.SUBMITTED) return result(attempt);
        var questions = questionRepository.findAllByExamId(attempt.examId());
        var answers = answerRepository.findAllByAttemptId(attempt.id());
        var automaticMax = questions.stream().filter(q -> q.type() == ExamQuestionType.MULTIPLE_CHOICE).mapToInt(ExamQuestion::points).sum();
        var automaticScore = questions.stream().filter(q -> q.type() == ExamQuestionType.MULTIPLE_CHOICE)
                .filter(question -> answers.stream().anyMatch(answer -> answer.questionId().equals(question.id())
                        && Objects.equals(answer.selectedOptionIndex(), question.correctOptionIndex())))
                .mapToInt(ExamQuestion::points).sum();
        var pendingCoding = (int) questions.stream().filter(q -> q.type() == ExamQuestionType.CODING).count();
        var submitted = attemptRepository.save(attempt.submit(Instant.now(clock), automaticScore, automaticMax, pendingCoding));
        return new ExamResult(submitted, answers);
    }

    @Override
    public ExamResult getResult(UUID studentId, UUID attemptId) {
        var attempt = ownedAttempt(studentId, attemptId);
        if (attempt.status() != ExamAttemptStatus.SUBMITTED) throw new ExamStateException("Exam attempt has not been submitted");
        return result(attempt);
    }

    @Override
    public List<ExamResult> listResults(UUID actorId, UserRole actorRole, UUID examId) {
        var exam = getExam(examId);
        requireManager(exam, actorId, actorRole);
        return attemptRepository.findAllByExamId(exam.id()).stream().map(this::result).toList();
    }

    private ExamResult result(ExamAttempt attempt) { return new ExamResult(attempt, answerRepository.findAllByAttemptId(attempt.id())); }
    private ExamDetails details(Exam exam) { return new ExamDetails(exam, questionRepository.findAllByExamId(exam.id())); }
    private Exam getExam(UUID id) { return examRepository.findById(Objects.requireNonNull(id, "Exam id is required")).orElseThrow(() -> new ExamResourceNotFoundException("Exam", id)); }
    private ExamAttempt ownedAttempt(UUID studentId, UUID attemptId) {
        Objects.requireNonNull(studentId, "Student id is required");
        var attempt = attemptRepository.findById(Objects.requireNonNull(attemptId, "Attempt id is required"))
                .orElseThrow(() -> new ExamResourceNotFoundException("Exam attempt", attemptId));
        if (!attempt.studentId().equals(studentId)) throw new ExamForbiddenException();
        return attempt;
    }
    private void requireManager(Exam exam, UUID actorId, UserRole role) {
        requireTeacher(role); Objects.requireNonNull(actorId, "Actor id is required");
        if (role != UserRole.ADMIN && !exam.teacherId().equals(actorId)) throw new ExamForbiddenException();
    }
    private void requireTeacher(UserRole role) { if (role != UserRole.TEACHER && role != UserRole.ADMIN) throw new ExamForbiddenException(); }
    private void validateAnswer(ExamQuestion question, Integer selected, String source) {
        if (question.type() == ExamQuestionType.MULTIPLE_CHOICE) {
            if (selected == null || selected < 0 || selected >= question.options().size()) throw new IllegalArgumentException("Selected option index is invalid");
            if (source != null && !source.isBlank()) throw new IllegalArgumentException("Multiple choice answer cannot contain source code");
        } else {
            if (selected != null) throw new IllegalArgumentException("Coding answer cannot contain a selected option");
            if (source == null || source.isBlank()) throw new IllegalArgumentException("Source code is required");
            if (source.length() > MAX_SOURCE_CODE_LENGTH) throw new IllegalArgumentException("Source code must not exceed 100000 characters");
        }
    }
}
