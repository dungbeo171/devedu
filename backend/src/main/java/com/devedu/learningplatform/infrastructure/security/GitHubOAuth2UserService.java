package com.devedu.learningplatform.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;

@Component
public class GitHubOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;

    public GitHubOAuth2UserService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        var user = delegate.loadUser(request);
        if (!"github".equals(request.getClientRegistration().getRegistrationId())) {
            return user;
        }

        var attributes = new LinkedHashMap<>(user.getAttributes());
        var email = verifiedGitHubEmail(request);
        attributes.put("email", email);
        attributes.put("email_verified_by_provider", true);
        var nameAttribute = request.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();
        return new DefaultOAuth2User(user.getAuthorities(), attributes, nameAttribute);
    }

    private String verifiedGitHubEmail(OAuth2UserRequest request) {
        try {
            var httpRequest = HttpRequest.newBuilder(URI.create("https://api.github.com/user/emails"))
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", "Bearer " + request.getAccessToken().getTokenValue())
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .GET()
                    .build();
            var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw invalidEmail();
            var emails = objectMapper.readTree(response.body());
            for (var email : emails) {
                if (email.path("primary").asBoolean() && email.path("verified").asBoolean()) {
                    return email.path("email").asText();
                }
            }
            for (var email : emails) {
                if (email.path("verified").asBoolean()) return email.path("email").asText();
            }
            throw invalidEmail();
        } catch (OAuth2AuthenticationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("github_email_unavailable"),
                    "GitHub did not provide a verified email",
                    exception
            );
        }
    }

    private OAuth2AuthenticationException invalidEmail() {
        return new OAuth2AuthenticationException(
                new OAuth2Error("github_email_unavailable"),
                "GitHub did not provide a verified email"
        );
    }
}
