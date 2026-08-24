package com.devedu.learningplatform.infrastructure.config;

import com.devedu.learningplatform.infrastructure.security.OAuthClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.ArrayList;

@Configuration
public class OAuthClientConfiguration {

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(OAuthClientSettings settings) {
        var registrations = new ArrayList<ClientRegistration>();
        if (settings.googleEnabled()) {
            registrations.add(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                    .clientId(settings.googleClientId())
                    .clientSecret(settings.googleClientSecret())
                    .scope("openid", "profile", "email")
                    .redirectUri(settings.redirectUri("google"))
                    .build());
        }
        if (settings.githubEnabled()) {
            registrations.add(CommonOAuth2Provider.GITHUB.getBuilder("github")
                    .clientId(settings.githubClientId())
                    .clientSecret(settings.githubClientSecret())
                    .scope("read:user", "user:email")
                    .redirectUri(settings.redirectUri("github"))
                    .build());
        }
        if (settings.microsoftEnabled()) {
            registrations.add(microsoft(settings));
        }
        return new OptionalClientRegistrationRepository(registrations);
    }

    private ClientRegistration microsoft(OAuthClientSettings settings) {
        return ClientRegistration.withRegistrationId("microsoft")
                .clientId(settings.microsoftClientId())
                .clientSecret(settings.microsoftClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(settings.redirectUri("microsoft"))
                .scope("openid", "profile", "email")
                .authorizationUri("https://login.microsoftonline.com/common/oauth2/v2.0/authorize")
                .tokenUri("https://login.microsoftonline.com/common/oauth2/v2.0/token")
                .jwkSetUri("https://login.microsoftonline.com/common/discovery/v2.0/keys")
                .userInfoUri("https://graph.microsoft.com/oidc/userinfo")
                .userNameAttributeName("sub")
                .clientName("Microsoft")
                .build();
    }

    public static final class OptionalClientRegistrationRepository
            implements ClientRegistrationRepository, Iterable<ClientRegistration> {
        private final java.util.Map<String, ClientRegistration> registrations;

        OptionalClientRegistrationRepository(java.util.List<ClientRegistration> registrations) {
            this.registrations = registrations.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                    ClientRegistration::getRegistrationId,
                    registration -> registration
            ));
        }

        @Override
        public ClientRegistration findByRegistrationId(String registrationId) {
            return registrations.get(registrationId);
        }

        @Override
        public java.util.Iterator<ClientRegistration> iterator() {
            return registrations.values().iterator();
        }
    }
}
