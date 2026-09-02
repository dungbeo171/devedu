package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.port.in.AdminUserManagementUseCase;
import com.devedu.learningplatform.application.port.in.command.UpdateUserRoleCommand;
import com.devedu.learningplatform.application.port.in.command.CreateManagedUserCommand;
import com.devedu.learningplatform.application.port.in.command.UpdateManagedUserCommand;
import com.devedu.learningplatform.application.port.in.command.DeleteManagedUserCommand;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.presentation.rest.dto.UpdateUserRoleRequest;
import com.devedu.learningplatform.presentation.rest.dto.UserResponse;
import com.devedu.learningplatform.presentation.rest.dto.CreateManagedUserRequest;
import com.devedu.learningplatform.presentation.rest.dto.UpdateManagedUserRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUsersController {

    private final AdminUserManagementUseCase useCase;

    public AdminUsersController(AdminUserManagementUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public List<UserResponse> listUsers(@AuthenticationPrincipal AuthenticatedUser actor) {
        return useCase.listUsers(actor.role()).stream().map(UserResponse::from).toList();
    }

    @PatchMapping("/{userId}/role")
    public UserResponse updateRole(
            @PathVariable long userId,
            @RequestBody UpdateUserRoleRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        return UserResponse.from(useCase.updateRole(new UpdateUserRoleCommand(
                actor.id(), actor.role(), userId, request.role()
        )));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @RequestBody CreateManagedUserRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var user = useCase.createUser(new CreateManagedUserCommand(
                actor.role(), request.name(), request.email(), request.password(), request.role()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PutMapping("/{userId}")
    public UserResponse updateUser(
            @PathVariable long userId,
            @RequestBody UpdateManagedUserRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        return UserResponse.from(useCase.updateUser(new UpdateManagedUserCommand(
                actor.id(), actor.role(), userId, request.name(), request.email(), request.password(), request.role()
        )));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable long userId,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        useCase.deleteUser(new DeleteManagedUserCommand(actor.id(), actor.role(), userId));
        return ResponseEntity.noContent().build();
    }
}
