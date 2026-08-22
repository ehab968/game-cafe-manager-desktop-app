package com.gamecafe.gamecafemanager.domain.usecase.user;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.domain.exception.UserNotFoundException;
import com.gamecafe.gamecafemanager.domain.model.Permission;
import com.gamecafe.gamecafemanager.domain.model.Role;
import com.gamecafe.gamecafemanager.domain.model.User;
import com.gamecafe.gamecafemanager.domain.repository.UserRepository;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import java.util.Objects;

public final class SetUserEnabledUseCase {

    private final UserRepository repository;
    private final AuthorizationService authorization;

    public SetUserEnabledUseCase(UserRepository repository, AuthorizationService authorization) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    public void execute(long userId, boolean enabled) {
        authorization.require(Permission.MANAGE_USERS);
        User user = repository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        User current = authorization.requireAuthenticatedUser();
        if (current.getId() == userId && !enabled) {
            throw ValidationException.forField("enabled", "You cannot disable your own account");
        }
        if (!enabled
                && user.isEnabled()
                && user.getRole() == Role.ADMIN
                && repository.countEnabledAdmins() <= 1) {
            throw ValidationException.forField(
                    "enabled", "At least one enabled administrator is required");
        }
        repository.setEnabled(userId, enabled);
    }
}
