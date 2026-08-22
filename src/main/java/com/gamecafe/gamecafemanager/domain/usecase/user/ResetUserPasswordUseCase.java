package com.gamecafe.gamecafemanager.domain.usecase.user;

import com.gamecafe.gamecafemanager.domain.exception.UserNotFoundException;
import com.gamecafe.gamecafemanager.domain.model.Permission;
import com.gamecafe.gamecafemanager.domain.repository.UserRepository;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import com.gamecafe.gamecafemanager.domain.service.PasswordHasher;
import com.gamecafe.gamecafemanager.domain.service.UserValidator;
import java.util.Objects;

public final class ResetUserPasswordUseCase {

    private final UserRepository repository;
    private final PasswordHasher passwordHasher;
    private final UserValidator validator;
    private final AuthorizationService authorization;

    public ResetUserPasswordUseCase(
            UserRepository repository,
            PasswordHasher passwordHasher,
            UserValidator validator,
            AuthorizationService authorization) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    public void execute(long userId, char[] password) {
        authorization.require(Permission.MANAGE_USERS);
        validator.validatePassword(password);
        if (!repository.findById(userId).isPresent()) {
            throw new UserNotFoundException(userId);
        }
        repository.updatePassword(userId, passwordHasher.hash(password));
    }
}
