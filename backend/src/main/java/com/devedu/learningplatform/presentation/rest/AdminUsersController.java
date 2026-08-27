package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.port.in.AdminUserManagementUseCase;
import com.devedu.learningplatform.application.port.in.command.UpdateUserRoleCommand;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.presentation.rest.dto.UpdateUserRoleRequest;
import com.devedu.learningplatform.presentation.rest.dto.UserResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

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
            @PathVariable UUID userId,
            @RequestBody UpdateUserRoleRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        return UserResponse.from(useCase.updateRole(new UpdateUserRoleCommand(
                actor.id(), actor.role(), userId, request.role()
        )));
    }
}
