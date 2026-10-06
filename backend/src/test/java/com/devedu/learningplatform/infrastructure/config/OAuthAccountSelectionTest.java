package com.devedu.learningplatform.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.*;

class OAuthAccountSelectionTest {
    private final OAuth2AuthorizationRequestResolver resolver = new OAuthClientConfiguration()
            .authorizationRequestResolver(new InMemoryClientRegistrationRepository(
                    CommonOAuth2Provider.GOOGLE.getBuilder("google")
                            .clientId("test-google-client").clientSecret("test-only-secret")
                            .scope("openid", "profile", "email")
                            .redirectUri("https://devedu.test/login/oauth2/code/google").build(),
                    CommonOAuth2Provider.GITHUB.getBuilder("github")
                            .clientId("test-github-client").clientSecret("test-only-secret")
                            .scope("read:user", "user:email")
                            .redirectUri("https://devedu.test/login/oauth2/code/github").build()));

    @ParameterizedTest
    @ValueSource(strings = {"google", "github"})
    void alwaysRequestsAccountSelectionWithoutChangingOAuthSecurity(String provider) {
        var request = request(provider);
        request.addParameter("prompt", "none"); // Client cannot bypass the account picker.
        var resolved = resolver.resolve(request);
        assertThat(resolved).isNotNull();
        assertThat(resolved.getAdditionalParameters()).containsEntry("prompt", "select_account");
        assertThat(UriComponentsBuilder.fromUriString(resolved.getAuthorizationRequestUri())
                .build().getQueryParams().get("prompt")).containsExactly("select_account");
        assertThat(resolved.getRedirectUri()).isEqualTo("https://devedu.test/login/oauth2/code/" + provider);
        assertThat(resolved.getState()).isNotBlank();
        assertThat(resolved.getState()).isNotEqualTo(resolver.resolve(request(provider)).getState());
        if (provider.equals("google")) {
            assertThat(resolved.getScopes()).containsExactlyInAnyOrder("openid", "profile", "email");
            assertThat(resolved.getAdditionalParameters()).containsKey("nonce");
        } else {
            assertThat(resolved.getScopes()).containsExactlyInAnyOrder("read:user", "user:email");
        }
        assertThat(resolved.getAuthorizationRequestUri()).doesNotContain("test-only-secret");
    }

    @ParameterizedTest
    @ValueSource(strings = {"google", "github"})
    void alsoSelectsAccountWhenResolvingByRegistrationId(String provider) {
        assertThat(resolver.resolve(new MockHttpServletRequest(), provider).getAdditionalParameters())
                .containsEntry("prompt", "select_account");
    }

    @Test
    void ignoresNonOAuthPathsAndRejectsUnconfiguredProviders() {
        assertThat(resolver.resolve(new MockHttpServletRequest("GET", "/api/problems"))).isNull();
        assertThatThrownBy(() -> resolver.resolve(request("unknown")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private MockHttpServletRequest request(String provider) {
        var path = "/oauth2/authorization/" + provider;
        var request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        return request;
    }
}
