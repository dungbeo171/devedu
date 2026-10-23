package com.devedu.learningplatform.presentation.rest.dto;

public record RegistrationVerificationResponse(
        String email,
        long expiresInSeconds,
        long resendAfterSeconds
) {
}
