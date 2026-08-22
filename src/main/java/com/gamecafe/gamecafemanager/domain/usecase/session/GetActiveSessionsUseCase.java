package com.gamecafe.gamecafemanager.domain.usecase.session;

import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.model.Permission;
import com.gamecafe.gamecafemanager.domain.repository.SessionRepository;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import java.util.List;
import java.util.Objects;

public final class GetActiveSessionsUseCase {

    private final SessionRepository repository;
    private final AuthorizationService authorization;

    public GetActiveSessionsUseCase(
            SessionRepository repository,
            AuthorizationService authorization) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    public List<Session> execute() {
        authorization.require(Permission.OPERATE_SESSIONS);
        return repository.findActive();
    }
}
