package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.port.in.AuthenticationUseCase;
import com.devedu.learningplatform.application.port.in.ListExternalAuthProvidersUseCase;
import com.devedu.learningplatform.application.port.in.command.LoginCommand;
import com.devedu.learningplatform.application.port.in.command.RegisterUserCommand;
import com.devedu.learningplatform.application.port.in.result.AuthenticationResult;
import com.devedu.learningplatform.presentation.rest.dto.AuthenticationResponse;
import com.devedu.learningplatform.presentation.rest.dto.OAuthProvidersResponse;
import com.devedu.learningplatform.presentation.rest.dto.LoginRequest;
import com.devedu.learningplatform.presentation.rest.dto.RegisterRequest;
import com.devedu.learningplatform.presentation.rest.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationUseCase authenticationUseCase;
    private final ListExternalAuthProvidersUseCase providersUseCase;

    public AuthenticationController(AuthenticationUseCase authenticationUseCase,
                                    ListExternalAuthProvidersUseCase providersUseCase) {
        this.authenticationUseCase = authenticationUseCase;
        this.providersUseCase = providersUseCase;
    }

    @org.springframework.web.bind.annotation.GetMapping("/oauth/providers")
    public OAuthProvidersResponse providers() {
        return new OAuthProvidersResponse(providersUseCase.listEnabledProviders());
    }

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest request) {
        var result = authenticationUseCase.register(new RegisterUserCommand(request.email(), request.password()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(toResponse(result));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(toResponse(authenticationUseCase.login(new LoginCommand(request.email(), request.password()))));
    }

    private AuthenticationResponse toResponse(AuthenticationResult result) {
        var user = result.user();
        return new AuthenticationResponse(
                result.accessToken().value(),
                "Bearer",
                result.accessToken().expiresInSeconds(),
                new UserResponse(user.id(), user.email(), user.role(), user.createdAt())
        );
    }
}
