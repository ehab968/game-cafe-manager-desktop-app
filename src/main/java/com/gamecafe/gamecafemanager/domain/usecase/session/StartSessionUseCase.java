package com.gamecafe.gamecafemanager.domain.usecase.session;

import com.gamecafe.gamecafemanager.domain.exception.ActiveSessionAlreadyExistsException;
import com.gamecafe.gamecafemanager.domain.exception.StationDisabledException;
import com.gamecafe.gamecafemanager.domain.exception.StationNotFoundException;
import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.model.SessionStatus;
import com.gamecafe.gamecafemanager.domain.model.Station;
import com.gamecafe.gamecafemanager.domain.repository.SessionRepository;
import com.gamecafe.gamecafemanager.domain.repository.StationRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.Objects;

public final class StartSessionUseCase {

    private static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

    private final StationRepository stationRepository;
    private final SessionRepository sessionRepository;
    private final Clock clock;

    public StartSessionUseCase(
            StationRepository stationRepository,
            SessionRepository sessionRepository,
            Clock clock) {
        this.stationRepository = Objects.requireNonNull(stationRepository, "stationRepository");
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Session execute(long stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new StationNotFoundException(stationId));
        if (!station.isEnabled()) {
            throw new StationDisabledException(stationId);
        }
        if (sessionRepository.findActiveByStationId(stationId).isPresent()) {
            throw new ActiveSessionAlreadyExistsException(stationId);
        }

        Session activeSession = new Session(
                null,
                stationId,
                station.getName(),
                clock.instant(),
                null,
                SessionStatus.ACTIVE,
                station.getHourlyRate(),
                ZERO_MONEY,
                ZERO_MONEY,
                ZERO_MONEY);
        return sessionRepository.create(activeSession);
    }
}
