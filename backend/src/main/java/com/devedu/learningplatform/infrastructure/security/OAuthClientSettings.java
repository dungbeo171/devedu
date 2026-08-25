package com.devedu.learningplatform.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OAuthClientSettings {
    private final String googleClientId;
    private final String googleClientSecret;
    private final String githubClientId;
    private final String githubClientSecret;
    private final String redirectBaseUri;
    private final String frontendCallbackUri;

    public OAuthClientSettings(
            @Value("${security.oauth.google.client-id:}") String googleClientId,
            @Value("${security.oauth.google.client-secret:}") String googleClientSecret,
            @Value("${security.oauth.github.client-id:}") String githubClientId,
            @Value("${security.oauth.github.client-secret:}") String githubClientSecret,
            @Value("${security.oauth.redirect-base-uri}") String redirectBaseUri,
            @Value("${security.oauth.frontend-callback-uri}") String frontendCallbackUri
    ) {
        this.googleClientId = googleClientId.trim();
        this.googleClientSecret = googleClientSecret.trim();
        this.githubClientId = githubClientId.trim();
        this.githubClientSecret = githubClientSecret.trim();
        this.redirectBaseUri = redirectBaseUri.replaceAll("/+$", "");
        this.frontendCallbackUri = frontendCallbackUri;
    }

    public boolean googleEnabled() { return configured(googleClientId, googleClientSecret); }
    public boolean githubEnabled() { return configured(githubClientId, githubClientSecret); }
    public boolean anyEnabled() { return googleEnabled() || githubEnabled(); }

    public List<String> enabledProviders() {
        var providers = new ArrayList<String>();
        if (googleEnabled()) providers.add("google");
        if (githubEnabled()) providers.add("github");
        return List.copyOf(providers);
    }

    public String googleClientId() { return googleClientId; }
    public String googleClientSecret() { return googleClientSecret; }
    public String githubClientId() { return githubClientId; }
    public String githubClientSecret() { return githubClientSecret; }
    public String redirectUri(String registrationId) { return redirectBaseUri + "/" + registrationId; }
    public String frontendCallbackUri() { return frontendCallbackUri; }

    private boolean configured(String clientId, String clientSecret) {
        return !clientId.isBlank() && !clientSecret.isBlank();
    }
}
