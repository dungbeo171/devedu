package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.port.in.AdminEmailVerificationSettingsUseCase;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/settings/email-verification")
public class AdminEmailVerificationSettingsController {

    private final AdminEmailVerificationSettingsUseCase useCase;

    public AdminEmailVerificationSettingsController(AdminEmailVerificationSettingsUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public ResponseEntity<SettingsResponse> get(@AuthenticationPrincipal AuthenticatedUser actor) {
        return noStore(toResponse(useCase.get(actor.role())));
    }

    @PutMapping
    public ResponseEntity<SettingsResponse> update(
            @RequestBody UpdateSettingsRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        return noStore(toResponse(useCase.update(request.enabled(), actor.role())));
    }

    private SettingsResponse toResponse(AdminEmailVerificationSettingsUseCase.Settings settings) {
        return new SettingsResponse(settings.enabled(), settings.smtpConfigured());
    }

    private ResponseEntity<SettingsResponse> noStore(SettingsResponse response) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    public record UpdateSettingsRequest(boolean enabled) {
    }

    public record SettingsResponse(boolean enabled, boolean smtpConfigured) {
    }
}
