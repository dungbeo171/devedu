package com.devedu.learningplatform.application.port.in;

import com.devedu.learningplatform.domain.model.InterviewDifficulty;
import com.devedu.learningplatform.domain.model.InterviewQuestion;
import com.devedu.learningplatform.domain.model.InterviewTopic;
import java.util.List; import java.util.UUID;

public interface InterviewQuestionsUseCase {
    List<InterviewQuestion> list(InterviewTopic topic, InterviewDifficulty difficulty);
    InterviewQuestion getById(UUID id);
}
