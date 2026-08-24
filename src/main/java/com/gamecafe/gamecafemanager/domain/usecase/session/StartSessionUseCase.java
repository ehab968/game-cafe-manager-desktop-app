package com.gamecafe.gamecafemanager.domain.usecase.session;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.domain.exception.ActiveSessionAlreadyExistsException;
import com.gamecafe.gamecafemanager.domain.exception.StationDisabledException;
import com.gamecafe.gamecafemanager.domain.exception.StationNotFoundException;
import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.model.SessionMode;
import com.gamecafe.gamecafemanager.domain.model.Permission;
import com.gamecafe.gamecafemanager.domain.model.SessionStatus;
import com.gamecafe.gamecafemanager.domain.model.Station;
import com.gamecafe.gamecafemanager.domain.repository.SessionRepository;
import com.gamecafe.gamecafemanager.domain.repository.StationRepository;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.Objects;

public final class StartSessionUseCase {

    private static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

    private final StationRepository stationRepository;
    private final SessionRepository sessionRepository;
    private final Clock clock;
    private final AuthorizationService authorization;

    public StartSessionUseCase(
            StationRepository stationRepository,
            SessionRepository sessionRepository,
            Clock clock,
            AuthorizationService authorization) {
        this.stationRepository = Objects.requireNonNull(stationRepository, "stationRepository");
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    public Session execute(long stationId) {
        return execute(stationId, null);
    }

    public Session execute(long stationId, SessionMode mode) {
        authorization.require(Permission.OPERATE_SESSIONS);
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new StationNotFoundException(stationId));
        if (!station.isEnabled()) {
            throw new StationDisabledException(stationId);
        }
        if (sessionRepository.findActiveByStationId(stationId).isPresent()) {
            throw new ActiveSessionAlreadyExistsException(stationId);
        }
        validateMode(station, mode);

        Session activeSession = new Session(
                null,
                stationId,
                station.getName(),
                station.getType(),
                mode,
                clock.instant(),
                null,
                SessionStatus.ACTIVE,
                station.getRateForMode(mode),
                ZERO_MONEY,
                ZERO_MONEY,
                ZERO_MONEY);
        return sessionRepository.create(activeSession);
    }

    private void validateMode(Station station, SessionMode mode) {
        if (station.getType().supportsSessionModes() && mode == null) {
            throw ValidationException.forField(
                    "sessionMode",
                    "Select Single or Multi before starting this station");
        }
        if (!station.getType().supportsSessionModes() && mode != null) {
            throw ValidationException.forField(
                    "sessionMode",
                    "This station type does not use Single or Multi mode");
        }
    }
}
