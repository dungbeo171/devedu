package com.devedu.learningplatform.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuthLoginFailureHandler implements AuthenticationFailureHandler {
    private final OAuthClientSettings settings;

    public OAuthLoginFailureHandler(OAuthClientSettings settings) {
        this.settings = settings;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        response.setHeader("Cache-Control", "no-store");
        response.sendRedirect(settings.frontendCallbackUri() + "?oauth_error=provider_login_failed");
    }
}
