package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.domain.model.InterviewDifficulty;
import com.devedu.learningplatform.domain.model.InterviewQuestion;
import com.devedu.learningplatform.domain.model.InterviewTopic;
import java.util.List; import java.util.Optional; import java.util.UUID;

public interface InterviewQuestionRepository {
    List<InterviewQuestion> findAll(InterviewTopic topic, InterviewDifficulty difficulty);
    Optional<InterviewQuestion> findById(UUID id);
}
