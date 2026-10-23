package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.analytics.AdminAnalyticsUseCase;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.presentation.rest.dto.AdminAnalyticsResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {
    private final AdminAnalyticsUseCase useCase;

    public AdminAnalyticsController(AdminAnalyticsUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public ResponseEntity<AdminAnalyticsResponse> dashboard(@AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(AdminAnalyticsResponse.from(useCase.dashboard(actor.role())));
    }
}
