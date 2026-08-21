package com.gamecafe.gamecafemanager.domain.usecase.station;

import com.gamecafe.gamecafemanager.domain.model.Station;
import com.gamecafe.gamecafemanager.domain.repository.StationRepository;
import java.util.List;
import java.util.Objects;

public final class GetStationsUseCase {

    private final StationRepository repository;

    public GetStationsUseCase(StationRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public List<Station> execute() {
        return repository.findAll();
    }
}
