package com.gamecafe.gamecafemanager.domain.usecase.station;

import com.gamecafe.gamecafemanager.domain.exception.StationNotFoundException;
import com.gamecafe.gamecafemanager.domain.model.Permission;
import com.gamecafe.gamecafemanager.domain.repository.StationRepository;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import java.util.Objects;

public final class SetStationEnabledUseCase {

    private final StationRepository repository;
    private final AuthorizationService authorization;

    public SetStationEnabledUseCase(
            StationRepository repository,
            AuthorizationService authorization) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    public void execute(long id, boolean enabled) {
        authorization.require(Permission.MANAGE_STATIONS);
        if (!repository.findById(id).isPresent()) {
            throw new StationNotFoundException(id);
        }
        repository.setEnabled(id, enabled);
    }
}
