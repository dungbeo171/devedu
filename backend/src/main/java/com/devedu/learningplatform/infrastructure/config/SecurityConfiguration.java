package com.devedu.learningplatform.infrastructure.config;

import com.devedu.learningplatform.infrastructure.security.JwtAuthenticationFilter;
import com.devedu.learningplatform.infrastructure.security.GitHubOAuth2UserService;
import com.devedu.learningplatform.infrastructure.security.OAuthLoginFailureHandler;
import com.devedu.learningplatform.infrastructure.security.OAuthLoginSuccessHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;

@Configuration
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;
    private final GitHubOAuth2UserService githubOAuth2UserService;
    private final OAuthLoginSuccessHandler oauthSuccessHandler;
    private final OAuthLoginFailureHandler oauthFailureHandler;

    public SecurityConfiguration(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper,
                                 ObjectProvider<GitHubOAuth2UserService> githubOAuth2UserService,
                                 ObjectProvider<OAuthLoginSuccessHandler> oauthSuccessHandler,
                                 ObjectProvider<OAuthLoginFailureHandler> oauthFailureHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
        this.githubOAuth2UserService = githubOAuth2UserService.getIfAvailable();
        this.oauthSuccessHandler = oauthSuccessHandler.getIfAvailable();
        this.oauthFailureHandler = oauthFailureHandler.getIfAvailable();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/oauth/providers",
                                "/oauth2/authorization/*",
                                "/login/oauth2/code/*",
                                "/api/system/status",
                                "/api/code/execute",
                                "/error"
                        )
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/problems", "/api/problems/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/courses", "/api/courses/*", "/api/lessons/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/problems/*/submissions").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/student/problem-progress").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/student/problems/*/draft").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.PUT, "/api/student/problems/*/draft").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.POST, "/api/student/lessons/*/complete").hasRole("STUDENT")
                        .requestMatchers("/api/exams", "/api/exams/**").hasRole("STUDENT")
                        .requestMatchers("/api/interview", "/api/interview/**").hasRole("STUDENT")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/teacher/**").hasAnyRole("TEACHER", "ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writeSecurityError(request.getRequestURI(), response,
                                        HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                        .accessDeniedHandler((request, response, exception) ->
                                writeSecurityError(request.getRequestURI(), response,
                                        HttpServletResponse.SC_FORBIDDEN, "Forbidden")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        if (githubOAuth2UserService != null && oauthSuccessHandler != null && oauthFailureHandler != null) {
            http.oauth2Login(oauth -> oauth
                    .userInfoEndpoint(userInfo -> userInfo.userService(githubOAuth2UserService))
                    .successHandler(oauthSuccessHandler)
                    .failureHandler(oauthFailureHandler));
        }
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${security.cors.allowed-origins}") String allowedOrigins
    ) {
        return buildCorsConfigurationSource(allowedOrigins);
    }

    private CorsConfigurationSource buildCorsConfigurationSource(String allowedOrigins) {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    private void writeSecurityError(String path, HttpServletResponse response, int status, String message)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        var body = new LinkedHashMap<String, Object>();
        body.put("timestamp", Instant.now());
        body.put("status", status);
        body.put("error", status == HttpServletResponse.SC_UNAUTHORIZED ? "Unauthorized" : "Forbidden");
        body.put("message", message);
        body.put("path", path);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
