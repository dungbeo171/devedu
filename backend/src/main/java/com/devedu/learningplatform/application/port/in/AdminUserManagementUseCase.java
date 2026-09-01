package com.devedu.learningplatform.application.port.in;

import com.devedu.learningplatform.application.port.in.command.UpdateUserRoleCommand;
import com.devedu.learningplatform.application.port.in.command.CreateManagedUserCommand;
import com.devedu.learningplatform.application.port.in.command.UpdateManagedUserCommand;
import com.devedu.learningplatform.application.port.in.command.DeleteManagedUserCommand;
import com.devedu.learningplatform.domain.model.User;
import com.devedu.learningplatform.domain.model.UserRole;

import java.util.List;

public interface AdminUserManagementUseCase {

    List<User> listUsers(UserRole actorRole);

    User updateRole(UpdateUserRoleCommand command);

    User createUser(CreateManagedUserCommand command);

    User updateUser(UpdateManagedUserCommand command);

    void deleteUser(DeleteManagedUserCommand command);

    void ensureBootstrapAdmin(String name, String email, String password);
}
