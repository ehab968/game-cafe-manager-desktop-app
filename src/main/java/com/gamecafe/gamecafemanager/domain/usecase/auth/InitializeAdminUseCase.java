package com.gamecafe.gamecafemanager.domain.usecase.auth;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.domain.model.Role;
import com.gamecafe.gamecafemanager.domain.model.User;
import com.gamecafe.gamecafemanager.domain.repository.UserRepository;
import com.gamecafe.gamecafemanager.domain.service.PasswordHasher;
import com.gamecafe.gamecafemanager.domain.service.UserValidator;
import java.util.Objects;

/**
 * One-time, unauthenticated creation of the first local administrator.
 */
public final class InitializeAdminUseCase {

    private final UserRepository repository;
    private final PasswordHasher passwordHasher;
    private final UserValidator validator;

    public InitializeAdminUseCase(
            UserRepository repository,
            PasswordHasher passwordHasher,
            UserValidator validator) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    public User execute(String username, char[] password) {
        if (repository.count() != 0) {
            throw new IllegalStateException("The first administrator has already been created");
        }
        validator.validate(username, Role.ADMIN, password);
        String normalized = validator.normalizeUsername(username);
        if (repository.existsByUsername(normalized)) {
            throw ValidationException.forField("username", "Username already exists");
        }
        return repository.create(
                new User(null, normalized, Role.ADMIN, true),
                passwordHasher.hash(password));
    }
}
