package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.exception.InterviewQuestionNotFoundException;
import com.devedu.learningplatform.application.port.out.InterviewQuestionRepository;
import com.devedu.learningplatform.domain.model.InterviewDifficulty;
import com.devedu.learningplatform.domain.model.InterviewQuestion;
import com.devedu.learningplatform.domain.model.InterviewTopic;
import org.junit.jupiter.api.Test;
import java.time.Instant; import java.util.List; import java.util.Optional; import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterviewQuestionsServiceTest {
    private final TestRepository repository = new TestRepository();
    private final InterviewQuestionsService service = new InterviewQuestionsService(repository);

    @Test void delegatesBothFiltersToRepository() {
        assertThat(service.list(InterviewTopic.JAVA, InterviewDifficulty.MEDIUM)).containsExactly(repository.question);
        assertThat(repository.topic).isEqualTo(InterviewTopic.JAVA);
        assertThat(repository.difficulty).isEqualTo(InterviewDifficulty.MEDIUM);
    }

    @Test void returnsQuestionDetailById() {
        assertThat(service.getById(repository.question.id()).answer()).isEqualTo("Dùng equals().");
    }

    @Test void reportsMissingQuestion() {
        var missing = UUID.randomUUID();
        assertThatThrownBy(() -> service.getById(missing)).isInstanceOf(InterviewQuestionNotFoundException.class).hasMessageContaining(missing.toString());
    }

    private static final class TestRepository implements InterviewQuestionRepository {
        private final InterviewQuestion question = new InterviewQuestion(UUID.randomUUID(), "So sánh String?", "Dùng equals().",
                "equals so sánh nội dung khi được override.", InterviewDifficulty.MEDIUM, InterviewTopic.JAVA, Instant.parse("2026-08-22T00:00:00Z"));
        private InterviewTopic topic; private InterviewDifficulty difficulty;
        @Override public List<InterviewQuestion> findAll(InterviewTopic topic, InterviewDifficulty difficulty) { this.topic=topic; this.difficulty=difficulty; return List.of(question); }
        @Override public Optional<InterviewQuestion> findById(UUID id) { return question.id().equals(id) ? Optional.of(question) : Optional.empty(); }
    }
}
