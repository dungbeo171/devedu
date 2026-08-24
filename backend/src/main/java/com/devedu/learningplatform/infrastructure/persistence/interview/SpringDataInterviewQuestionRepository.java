package com.devedu.learningplatform.infrastructure.persistence.interview;

import com.devedu.learningplatform.domain.model.InterviewDifficulty;
import com.devedu.learningplatform.domain.model.InterviewTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.UUID;

interface SpringDataInterviewQuestionRepository extends JpaRepository<InterviewQuestionJpaEntity, UUID> {
    List<InterviewQuestionJpaEntity> findAllByOrderByQuestionAsc();
    List<InterviewQuestionJpaEntity> findAllByTopicOrderByQuestionAsc(InterviewTopic topic);
    List<InterviewQuestionJpaEntity> findAllByDifficultyOrderByQuestionAsc(InterviewDifficulty difficulty);
    List<InterviewQuestionJpaEntity> findAllByTopicAndDifficultyOrderByQuestionAsc(InterviewTopic topic, InterviewDifficulty difficulty);
}
