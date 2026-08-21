package com.gamecafe.gamecafemanager.data.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gamecafe.gamecafemanager.data.sqlite.SQLiteDatabase;
import com.gamecafe.gamecafemanager.domain.exception.ActiveSessionAlreadyExistsException;
import com.gamecafe.gamecafemanager.domain.exception.StationDisabledException;
import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.model.SessionStatus;
import com.gamecafe.gamecafemanager.domain.model.Station;
import com.gamecafe.gamecafemanager.domain.model.StationType;
import com.gamecafe.gamecafemanager.domain.repository.SessionRepository;
import com.gamecafe.gamecafemanager.domain.repository.StationRepository;
import com.gamecafe.gamecafemanager.domain.service.PricingService;
import com.gamecafe.gamecafemanager.domain.service.StationValidator;
import com.gamecafe.gamecafemanager.domain.usecase.session.FinishSessionUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.session.GetActiveSessionsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.session.StartSessionUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.CreateStationUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.SetStationEnabledUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.UpdateStationUseCase;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SessionManagementIntegrationTest {

    private static final Instant START_TIME = Instant.parse("2026-08-21T12:00:00Z");

    @TempDir
    Path temporaryDirectory;

    private Path databasePath;
    private SQLiteDatabase database;
    private StationRepository stationRepository;
    private SessionRepository sessionRepository;
    private Station station;

    @BeforeEach
    void setUp() {
        databasePath = temporaryDirectory.resolve("sessions.db");
        database = new SQLiteDatabase(databasePath);
        database.initialize();
        stationRepository = new SQLiteStationRepository(database);
        sessionRepository = new SQLiteSessionRepository(database);

        station = new CreateStationUseCase(stationRepository, new StationValidator()).execute(
                "PlayStation Room 1",
                StationType.PLAYSTATION,
                new BigDecimal("120.00"));
    }

    @Test
    void startImmediatelyPersistsSessionAndSurvivesRepositoryReconstruction() {
        Session started = startSessionAt(START_TIME);

        assertNotNull(started.getId());
        assertEquals(START_TIME, started.getStartTime());
        assertNull(started.getEndTime());
        assertEquals(SessionStatus.ACTIVE, started.getStatus());
        assertEquals(new BigDecimal("120.00"), started.getHourlyRateSnapshot());

        SQLiteDatabase reconstructedDatabase = new SQLiteDatabase(databasePath);
        reconstructedDatabase.initialize();
        GetActiveSessionsUseCase reconstructedActiveSessions = new GetActiveSessionsUseCase(
                new SQLiteSessionRepository(reconstructedDatabase));

        assertEquals(1, reconstructedActiveSessions.execute().size());
        assertEquals(started.getId(), reconstructedActiveSessions.execute().get(0).getId());
    }

    @Test
    void preventsSecondActiveSessionForSameStation() {
        startSessionAt(START_TIME);

        assertThrows(
                ActiveSessionAlreadyExistsException.class,
                () -> startSessionAt(START_TIME.plusSeconds(30L)));
    }

    @Test
    void databaseConstraintAlsoPreventsDuplicateActiveSession() {
        Session started = startSessionAt(START_TIME);
        Session duplicate = new Session(
                null,
                started.getStationId(),
                started.getStationNameSnapshot(),
                START_TIME.plusSeconds(1L),
                null,
                SessionStatus.ACTIVE,
                started.getHourlyRateSnapshot(),
                new BigDecimal("0.00"),
                new BigDecimal("0.00"),
                new BigDecimal("0.00"));

        assertThrows(
                ActiveSessionAlreadyExistsException.class,
                () -> sessionRepository.create(duplicate));
    }

    @Test
    void finishUsesPersistedStartAndRateSnapshot() {
        Session started = startSessionAt(START_TIME);

        new UpdateStationUseCase(stationRepository, new StationValidator()).execute(
                station.getId(),
                station.getName(),
                station.getType(),
                new BigDecimal("200.00"));

        Instant finishTime = START_TIME.plusSeconds(5_400L);
        FinishSessionUseCase finishSession = new FinishSessionUseCase(
                sessionRepository,
                new PricingService(),
                fixedClock(finishTime));
        Session completed = finishSession.execute(started.getId());

        assertEquals(SessionStatus.COMPLETED, completed.getStatus());
        assertEquals(finishTime, completed.getEndTime());
        assertEquals(new BigDecimal("120.00"), completed.getHourlyRateSnapshot());
        assertEquals(new BigDecimal("180.00"), completed.getPlayCost());
        assertEquals(new BigDecimal("0.00"), completed.getProductsCost());
        assertEquals(new BigDecimal("180.00"), completed.getFinalTotal());
        assertTrue(new GetActiveSessionsUseCase(sessionRepository).execute().isEmpty());

        Session reloaded = sessionRepository.findById(completed.getId()).orElseThrow(AssertionError::new);
        assertEquals(new BigDecimal("180.00"), reloaded.getFinalTotal());
    }

    @Test
    void disabledStationCannotStartSession() {
        new SetStationEnabledUseCase(stationRepository).execute(station.getId(), false);

        assertThrows(StationDisabledException.class, () -> startSessionAt(START_TIME));
        assertFalse(sessionRepository.findActiveByStationId(station.getId()).isPresent());
    }

    private Session startSessionAt(Instant instant) {
        return new StartSessionUseCase(
                stationRepository,
                sessionRepository,
                fixedClock(instant))
                .execute(station.getId());
    }

    private Clock fixedClock(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }
}
