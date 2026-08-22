package com.gamecafe.gamecafemanager.domain.usecase.auth;

import com.gamecafe.gamecafemanager.domain.repository.UserRepository;
import java.util.Objects;

public final class HasUsersUseCase {

    private final UserRepository repository;

    public HasUsersUseCase(UserRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public boolean execute() {
        return repository.count() > 0;
    }
}
