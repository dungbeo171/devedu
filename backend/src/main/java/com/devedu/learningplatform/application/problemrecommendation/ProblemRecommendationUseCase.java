package com.devedu.learningplatform.application.problemrecommendation;

import java.util.List;
import java.util.UUID;
import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.ProblemDifficulty;
import com.devedu.learningplatform.domain.model.ProblemTopic;

public interface ProblemRecommendationUseCase {

    List<ProblemRecommendationView> recommend(
            UUID userId,
            ProblemTopic topic,
            ProblemDifficulty difficulty,
            CodeLanguage language
    );
}
