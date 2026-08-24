package com.devedu.learningplatform.presentation.rest.dto;

public record AuthenticationResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
}

