package com.devedu.learningplatform.application.problemrecommendation;

import com.devedu.learningplatform.domain.model.CodeLanguage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ProblemRecommendationSignalsPort {

    List<Activity> findByUserId(UUID userId);

    record Activity(
            UUID problemId,
            CodeLanguage language,
            long attempts,
            long acceptedAttempts,
            Instant lastSubmittedAt
    ) {
        public Activity {
            if (attempts < 1 || acceptedAttempts < 0 || acceptedAttempts > attempts) {
                throw new IllegalArgumentException("Invalid problem recommendation activity");
            }
        }

        public boolean solved() {
            return acceptedAttempts > 0;
        }
    }
}
