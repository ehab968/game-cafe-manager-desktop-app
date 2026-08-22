package com.gamecafe.gamecafemanager.domain.usecase.station;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.domain.model.Station;
import com.gamecafe.gamecafemanager.domain.model.Permission;
import com.gamecafe.gamecafemanager.domain.model.StationType;
import com.gamecafe.gamecafemanager.domain.repository.StationRepository;
import com.gamecafe.gamecafemanager.domain.service.StationValidator;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import java.math.BigDecimal;
import java.util.Objects;

public final class CreateStationUseCase {

    private final StationRepository repository;
    private final StationValidator validator;
    private final AuthorizationService authorization;

    public CreateStationUseCase(
            StationRepository repository,
            StationValidator validator,
            AuthorizationService authorization) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    public Station execute(String name, StationType type, BigDecimal hourlyRate) {
        authorization.require(Permission.MANAGE_STATIONS);
        validator.validate(name, type, hourlyRate);
        String normalizedName = validator.normalizeName(name);

        if (repository.existsByName(normalizedName, null)) {
            throw ValidationException.forField("name", "A station with this name already exists");
        }

        Station station = new Station(
                null,
                normalizedName,
                type,
                validator.normalizeHourlyRate(hourlyRate),
                true);
        return repository.create(station);
    }
}
