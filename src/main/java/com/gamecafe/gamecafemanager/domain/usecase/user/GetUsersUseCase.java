package com.gamecafe.gamecafemanager.domain.usecase.user;

import com.gamecafe.gamecafemanager.domain.model.Permission;
import com.gamecafe.gamecafemanager.domain.model.User;
import com.gamecafe.gamecafemanager.domain.repository.UserRepository;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import java.util.List;
import java.util.Objects;

public final class GetUsersUseCase {

    private final UserRepository repository;
    private final AuthorizationService authorization;

    public GetUsersUseCase(UserRepository repository, AuthorizationService authorization) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    public List<User> execute() {
        authorization.require(Permission.MANAGE_USERS);
        return repository.findAll();
    }
}
