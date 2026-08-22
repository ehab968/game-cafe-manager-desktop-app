package com.gamecafe.gamecafemanager.domain.usecase.user;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.domain.model.Permission;
import com.gamecafe.gamecafemanager.domain.model.Role;
import com.gamecafe.gamecafemanager.domain.model.User;
import com.gamecafe.gamecafemanager.domain.repository.UserRepository;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import com.gamecafe.gamecafemanager.domain.service.PasswordHasher;
import com.gamecafe.gamecafemanager.domain.service.UserValidator;
import java.util.Objects;

public final class CreateUserUseCase {

    private final UserRepository repository;
    private final PasswordHasher passwordHasher;
    private final UserValidator validator;
    private final AuthorizationService authorization;

    public CreateUserUseCase(
            UserRepository repository,
            PasswordHasher passwordHasher,
            UserValidator validator,
            AuthorizationService authorization) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    public User execute(String username, Role role, char[] password) {
        authorization.require(Permission.MANAGE_USERS);
        validator.validate(username, role, password);
        String normalized = validator.normalizeUsername(username);
        if (repository.existsByUsername(normalized)) {
            throw ValidationException.forField("username", "Username already exists");
        }
        return repository.create(
                new User(null, normalized, role, true),
                passwordHasher.hash(password));
    }
}
