package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.port.in.InterviewQuestionsUseCase;
import com.devedu.learningplatform.domain.model.InterviewDifficulty;
import com.devedu.learningplatform.domain.model.InterviewTopic;
import com.devedu.learningplatform.presentation.rest.dto.InterviewQuestionDetailResponse;
import com.devedu.learningplatform.presentation.rest.dto.InterviewQuestionSummaryResponse;
import org.springframework.web.bind.annotation.*;
import java.util.List; import java.util.UUID;

@RestController
@RequestMapping("/api/interview/questions")
public class InterviewQuestionsController {
    private final InterviewQuestionsUseCase useCase;
    public InterviewQuestionsController(InterviewQuestionsUseCase useCase) { this.useCase = useCase; }

    @GetMapping
    public List<InterviewQuestionSummaryResponse> list(@RequestParam(required=false) InterviewTopic topic,
                                                       @RequestParam(required=false) InterviewDifficulty difficulty) {
        return useCase.list(topic, difficulty).stream().map(InterviewQuestionSummaryResponse::from).toList();
    }

    @GetMapping("/{id}")
    public InterviewQuestionDetailResponse detail(@PathVariable UUID id) {
        return InterviewQuestionDetailResponse.from(useCase.getById(id));
    }
}
