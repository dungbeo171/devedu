package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.leaderboard.ProblemLeaderboardUseCase;
import com.devedu.learningplatform.domain.leaderboard.ProblemLeaderboardPeriod;
import com.devedu.learningplatform.presentation.rest.dto.ProblemLeaderboardResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaderboards/problems")
public class ProblemLeaderboardController {
    private final ProblemLeaderboardUseCase useCase;

    public ProblemLeaderboardController(ProblemLeaderboardUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public ProblemLeaderboardResponse get(
            @RequestParam(defaultValue = "WEEK") ProblemLeaderboardPeriod period
    ) {
        var view = useCase.get(period);
        return new ProblemLeaderboardResponse(
                view.period(), view.startsAt(), view.endsAt(), view.generatedAt(),
                new ProblemLeaderboardResponse.Scoring(
                        view.easyPoints(), view.mediumPoints(), view.hardPoints()),
                view.entries().stream().map(entry -> new ProblemLeaderboardResponse.Entry(
                        entry.rank(), entry.userId(), entry.name(), entry.role(), entry.score(), entry.solved(),
                        entry.easySolved(), entry.mediumSolved(), entry.hardSolved())).toList());
    }
}
