package com.devedu.learningplatform.application.port.in.result;

public record RegistrationVerificationResult(
        String email,
        long expiresInSeconds,
        long resendAfterSeconds
) {
}
