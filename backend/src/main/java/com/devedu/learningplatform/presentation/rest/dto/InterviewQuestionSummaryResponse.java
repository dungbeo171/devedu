package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.domain.model.InterviewDifficulty;
import com.devedu.learningplatform.domain.model.InterviewQuestion;
import com.devedu.learningplatform.domain.model.InterviewTopic;
import java.util.UUID;

public record InterviewQuestionSummaryResponse(UUID id, String question, InterviewDifficulty difficulty, InterviewTopic topic) {
    public static InterviewQuestionSummaryResponse from(InterviewQuestion question) {
        return new InterviewQuestionSummaryResponse(question.id(), question.question(), question.difficulty(), question.topic());
    }
}
