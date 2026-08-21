package com.gamecafe.gamecafemanager.domain.usecase.station;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.domain.exception.StationNotFoundException;
import com.gamecafe.gamecafemanager.domain.model.Station;
import com.gamecafe.gamecafemanager.domain.model.StationType;
import com.gamecafe.gamecafemanager.domain.repository.StationRepository;
import com.gamecafe.gamecafemanager.domain.service.StationValidator;
import java.math.BigDecimal;
import java.util.Objects;

public final class UpdateStationUseCase {

    private final StationRepository repository;
    private final StationValidator validator;

    public UpdateStationUseCase(StationRepository repository, StationValidator validator) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    public Station execute(long id, String name, StationType type, BigDecimal hourlyRate) {
        validator.validate(name, type, hourlyRate);
        Station existing = repository.findById(id)
                .orElseThrow(() -> new StationNotFoundException(id));
        String normalizedName = validator.normalizeName(name);

        if (repository.existsByName(normalizedName, id)) {
            throw ValidationException.forField("name", "A station with this name already exists");
        }

        return repository.update(new Station(
                id,
                normalizedName,
                type,
                validator.normalizeHourlyRate(hourlyRate),
                existing.isEnabled()));
    }
}
