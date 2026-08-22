package com.gamecafe.gamecafemanager.domain.usecase.user;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.domain.exception.UserNotFoundException;
import com.gamecafe.gamecafemanager.domain.model.Permission;
import com.gamecafe.gamecafemanager.domain.model.Role;
import com.gamecafe.gamecafemanager.domain.model.User;
import com.gamecafe.gamecafemanager.domain.repository.UserRepository;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import java.util.Objects;

public final class UpdateUserRoleUseCase {

    private final UserRepository repository;
    private final AuthorizationService authorization;

    public UpdateUserRoleUseCase(UserRepository repository, AuthorizationService authorization) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    public void execute(long userId, Role role) {
        authorization.require(Permission.MANAGE_USERS);
        Objects.requireNonNull(role, "role");
        User user = repository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        User current = authorization.requireAuthenticatedUser();
        if (current.getId() == userId && role != current.getRole()) {
            throw ValidationException.forField("role", "You cannot change your own role");
        }
        if (user.isEnabled()
                && user.getRole() == Role.ADMIN
                && role != Role.ADMIN
                && repository.countEnabledAdmins() <= 1) {
            throw ValidationException.forField("role", "At least one enabled administrator is required");
        }
        repository.updateRole(userId, role);
    }
}
