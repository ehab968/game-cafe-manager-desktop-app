package com.gamecafe.gamecafemanager.domain.usecase.station;

import com.gamecafe.gamecafemanager.domain.exception.StationNotFoundException;
import com.gamecafe.gamecafemanager.domain.repository.StationRepository;
import java.util.Objects;

public final class SetStationEnabledUseCase {

    private final StationRepository repository;

    public SetStationEnabledUseCase(StationRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public void execute(long id, boolean enabled) {
        if (!repository.findById(id).isPresent()) {
            throw new StationNotFoundException(id);
        }
        repository.setEnabled(id, enabled);
    }
}
