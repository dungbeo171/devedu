package com.devedu.learningplatform.infrastructure.security;

import com.devedu.learningplatform.application.port.in.AuthenticationUseCase;
import com.devedu.learningplatform.application.port.in.command.ExternalLoginCommand;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final AuthenticationUseCase authenticationUseCase;
    private final OAuthClientSettings settings;

    public OAuthLoginSuccessHandler(AuthenticationUseCase authenticationUseCase, OAuthClientSettings settings) {
        this.authenticationUseCase = authenticationUseCase;
        this.settings = settings;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        try {
            var oauth = (OAuth2AuthenticationToken) authentication;
            var email = verifiedEmail(oauth);
            var result = authenticationUseCase.loginExternal(new ExternalLoginCommand(email));
            invalidateSession(request);
            response.setHeader("Cache-Control", "no-store");
            var fragment = "access_token=" + encode(result.accessToken().value())
                    + "&token_type=Bearer"
                    + "&expires_in=" + result.accessToken().expiresInSeconds()
                    + "&email=" + encode(result.user().email())
                    + "&role=" + result.user().role().name();
            response.sendRedirect(settings.frontendCallbackUri() + "#" + fragment);
        } catch (RuntimeException exception) {
            invalidateSession(request);
            response.sendRedirect(settings.frontendCallbackUri() + "?oauth_error=external_login_failed");
        }
    }

    private String verifiedEmail(OAuth2AuthenticationToken oauth) {
        var provider = oauth.getAuthorizedClientRegistrationId();
        var principal = oauth.getPrincipal();
        if ("google".equals(provider)) {
            if (!(principal instanceof OidcUser oidcUser) || !Boolean.TRUE.equals(oidcUser.getEmailVerified())) {
                throw new IllegalArgumentException("Google email is not verified");
            }
            return oidcUser.getEmail();
        }
        if ("github".equals(provider)) {
            if (!Boolean.TRUE.equals(principal.getAttribute("email_verified_by_provider"))) {
                throw new IllegalArgumentException("GitHub email is not verified");
            }
            return principal.getAttribute("email");
        }
        if ("microsoft".equals(provider)) {
            var email = principal.<String>getAttribute("email");
            return email == null || email.isBlank() ? principal.getAttribute("preferred_username") : email;
        }
        throw new IllegalArgumentException("OAuth provider is unsupported");
    }

    private void invalidateSession(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) session.invalidate();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
