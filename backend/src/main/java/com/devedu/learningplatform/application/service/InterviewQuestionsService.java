package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.exception.InterviewQuestionNotFoundException;
import com.devedu.learningplatform.application.port.in.InterviewQuestionsUseCase;
import com.devedu.learningplatform.application.port.out.InterviewQuestionRepository;
import com.devedu.learningplatform.domain.model.InterviewDifficulty;
import com.devedu.learningplatform.domain.model.InterviewQuestion;
import com.devedu.learningplatform.domain.model.InterviewTopic;
import java.util.List; import java.util.Objects; import java.util.UUID;

public final class InterviewQuestionsService implements InterviewQuestionsUseCase {
    private final InterviewQuestionRepository repository;
    public InterviewQuestionsService(InterviewQuestionRepository repository) { this.repository = repository; }
    @Override public List<InterviewQuestion> list(InterviewTopic topic, InterviewDifficulty difficulty) { return repository.findAll(topic, difficulty); }
    @Override public InterviewQuestion getById(UUID id) { return repository.findById(Objects.requireNonNull(id, "Interview question id is required")).orElseThrow(() -> new InterviewQuestionNotFoundException(id)); }
}
