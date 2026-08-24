package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.InterviewDifficulty;
import com.devedu.learningplatform.domain.model.InterviewQuestion;
import com.devedu.learningplatform.domain.model.InterviewTopic;
import java.time.Instant; import java.util.UUID;

public record InterviewQuestionDetailResponse(UUID id, String question, String answer, String explanation,
                                              InterviewDifficulty difficulty, InterviewTopic topic, Instant createdAt) {
    public static InterviewQuestionDetailResponse from(InterviewQuestion question) {
        return new InterviewQuestionDetailResponse(question.id(), question.question(), question.answer(), question.explanation(), question.difficulty(), question.topic(), question.createdAt());
    }
}
