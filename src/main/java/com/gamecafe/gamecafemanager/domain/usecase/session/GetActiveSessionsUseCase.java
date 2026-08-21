package com.gamecafe.gamecafemanager.domain.usecase.session;

import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.repository.SessionRepository;
import java.util.List;
import java.util.Objects;

public final class GetActiveSessionsUseCase {

    private final SessionRepository repository;

    public GetActiveSessionsUseCase(SessionRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public List<Session> execute() {
        return repository.findActive();
    }
}
