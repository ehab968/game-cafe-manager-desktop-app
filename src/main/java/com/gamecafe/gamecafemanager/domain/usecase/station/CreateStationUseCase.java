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
        return executeInternal(name, type, hourlyRate, null);
    }

    public Station execute(
            String name,
            StationType type,
            BigDecimal singleHourlyRate,
            BigDecimal multiHourlyRate) {
        return executeInternal(name, type, singleHourlyRate, multiHourlyRate);
    }

    private Station executeInternal(
            String name,
            StationType type,
            BigDecimal primaryHourlyRate,
            BigDecimal multiHourlyRate) {
        authorization.require(Permission.MANAGE_STATIONS);
        validator.validate(name, type, primaryHourlyRate, multiHourlyRate);
        String normalizedName = validator.normalizeName(name);

        if (repository.existsByName(normalizedName, null)) {
            throw ValidationException.forField("name", "A station with this name already exists");
        }

        BigDecimal normalizedPrimaryRate = validator.normalizeHourlyRate(primaryHourlyRate);
        Station station = type.supportsSessionModes()
                ? new Station(
                        null,
                        normalizedName,
                        type,
                        normalizedPrimaryRate,
                        validator.normalizeHourlyRate(multiHourlyRate),
                        true)
                : new Station(null, normalizedName, type, normalizedPrimaryRate, true);
        return repository.create(station);
    }
}
