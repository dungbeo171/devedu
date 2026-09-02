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
            var identity = verifiedIdentity(oauth);
            var result = authenticationUseCase.loginExternal(new ExternalLoginCommand(identity.email(), identity.name()));
            invalidateSession(request);
            response.setHeader("Cache-Control", "no-store");
            var fragment = "access_token=" + encode(result.accessToken().value())
                    + "&token_type=Bearer"
                    + "&expires_in=" + result.accessToken().expiresInSeconds()
                    + "&name=" + encode(result.user().name())
                    + "&email=" + encode(result.user().email())
                    + "&role=" + result.user().role().name()
                    + "&public_id=" + result.user().publicId()
                    + "&student_code=" + encode(result.user().studentCode() == null ? "" : result.user().studentCode())
                    + "&teacher_code=" + encode(result.user().teacherCode() == null ? "" : result.user().teacherCode());
            response.sendRedirect(settings.frontendCallbackUri() + "#" + fragment);
        } catch (RuntimeException exception) {
            invalidateSession(request);
            response.sendRedirect(settings.frontendCallbackUri() + "?oauth_error=external_login_failed");
        }
    }

    private ExternalIdentity verifiedIdentity(OAuth2AuthenticationToken oauth) {
        var provider = oauth.getAuthorizedClientRegistrationId();
        var principal = oauth.getPrincipal();
        if ("google".equals(provider)) {
            if (!(principal instanceof OidcUser oidcUser) || !Boolean.TRUE.equals(oidcUser.getEmailVerified())) {
                throw new IllegalArgumentException("Google email is not verified");
            }
            return new ExternalIdentity(oidcUser.getEmail(), preferredName(oidcUser.getFullName(), oidcUser.getEmail()));
        }
        if ("github".equals(provider)) {
            if (!Boolean.TRUE.equals(principal.getAttribute("email_verified_by_provider"))) {
                throw new IllegalArgumentException("GitHub email is not verified");
            }
            var email = principal.<String>getAttribute("email");
            var name = principal.<String>getAttribute("name");
            if (name == null || name.isBlank()) name = principal.getAttribute("login");
            return new ExternalIdentity(email, preferredName(name, email));
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

    private String preferredName(String name, String email) {
        return name == null || name.isBlank() ? email.substring(0, email.indexOf('@')) : name;
    }

    private record ExternalIdentity(String email, String name) {
    }
}
