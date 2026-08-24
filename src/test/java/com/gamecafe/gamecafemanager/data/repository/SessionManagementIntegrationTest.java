package com.gamecafe.gamecafemanager.data.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.data.sqlite.SQLiteDatabase;
import com.gamecafe.gamecafemanager.domain.exception.ActiveSessionAlreadyExistsException;
import com.gamecafe.gamecafemanager.domain.exception.StationDisabledException;
import com.gamecafe.gamecafemanager.domain.exception.StationInUseException;
import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.model.SessionMode;
import com.gamecafe.gamecafemanager.domain.model.SessionStatus;
import com.gamecafe.gamecafemanager.domain.model.Station;
import com.gamecafe.gamecafemanager.domain.model.StationType;
import com.gamecafe.gamecafemanager.domain.repository.SessionRepository;
import com.gamecafe.gamecafemanager.domain.repository.SessionProductRepository;
import com.gamecafe.gamecafemanager.domain.repository.StationRepository;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import com.gamecafe.gamecafemanager.domain.service.PricingService;
import com.gamecafe.gamecafemanager.domain.service.CheckoutService;
import com.gamecafe.gamecafemanager.domain.service.StationValidator;
import com.gamecafe.gamecafemanager.domain.usecase.session.FinishSessionUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.session.GetActiveSessionsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.session.StartSessionUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.CreateStationUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.SetStationEnabledUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.UpdateStationUseCase;
import com.gamecafe.gamecafemanager.support.AuthenticationTestSupport;
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
    private SessionProductRepository sessionProductRepository;
    private AuthorizationService authorization;
    private Station station;

    @BeforeEach
    void setUp() {
        databasePath = temporaryDirectory.resolve("sessions.db");
        database = new SQLiteDatabase(databasePath);
        database.initialize();
        stationRepository = new SQLiteStationRepository(database);
        sessionRepository = new SQLiteSessionRepository(database);
        sessionProductRepository = new SQLiteSessionProductRepository(database);
        authorization = AuthenticationTestSupport.authenticatedAdmin(database);

        station = new CreateStationUseCase(
                stationRepository, new StationValidator(), authorization).execute(
                "PlayStation Room 1",
                StationType.PLAYSTATION,
                new BigDecimal("120.00"),
                new BigDecimal("160.00"));
    }

    @Test
    void startImmediatelyPersistsSessionAndSurvivesRepositoryReconstruction() {
        Session started = startSessionAt(START_TIME);

        assertNotNull(started.getId());
        assertEquals(START_TIME, started.getStartTime());
        assertNull(started.getEndTime());
        assertEquals(SessionStatus.ACTIVE, started.getStatus());
        assertEquals(SessionMode.SINGLE, started.getMode());
        assertEquals(new BigDecimal("120.00"), started.getHourlyRateSnapshot());

        new UpdateStationUseCase(
                stationRepository, new StationValidator(), authorization).execute(
                        station.getId(),
                        "Renamed Billiard",
                        StationType.BILLIARD,
                        new BigDecimal("200.00"));

        SQLiteDatabase reconstructedDatabase = new SQLiteDatabase(databasePath);
        reconstructedDatabase.initialize();
        SessionRepository reconstructedRepository =
                new SQLiteSessionRepository(reconstructedDatabase);
        GetActiveSessionsUseCase reconstructedActiveSessions = new GetActiveSessionsUseCase(
                reconstructedRepository, authorization);

        Session restored = reconstructedActiveSessions.execute()
                .stream()
                .findFirst()
                .orElseThrow(AssertionError::new);

        assertEquals(1, reconstructedActiveSessions.execute().size());
        assertEquals(started.getId(), restored.getId());
        assertEquals(station.getId().longValue(), restored.getStationId());
        assertEquals("PlayStation Room 1", restored.getStationNameSnapshot());
        assertEquals(StationType.PLAYSTATION, restored.getStationTypeSnapshot());
        assertEquals(SessionMode.SINGLE, restored.getMode());
        assertEquals(START_TIME, restored.getStartTime());
        assertNull(restored.getEndTime());
        assertEquals(SessionStatus.ACTIVE, restored.getStatus());
        assertEquals(new BigDecimal("120.00"), restored.getHourlyRateSnapshot());
        assertEquals(new BigDecimal("0.00"), restored.getFinalTotal());

        assertThrows(ActiveSessionAlreadyExistsException.class, () ->
                new StartSessionUseCase(
                        stationRepository,
                        reconstructedRepository,
                        fixedClock(START_TIME.plusSeconds(60L)),
                        authorization).execute(station.getId(), SessionMode.SINGLE));
    }

    @Test
    void preventsSecondActiveSessionForSameStation() {
        startSessionAt(START_TIME);

        assertThrows(
                ActiveSessionAlreadyExistsException.class,
                () -> startSessionAt(START_TIME.plusSeconds(30L)));
    }

    @Test
    void startsPlayStationSingleSessionWithSingleSnapshot() {
        Session started = startSessionAt(START_TIME, SessionMode.SINGLE);

        assertEquals(SessionMode.SINGLE, started.getMode());
        assertEquals(new BigDecimal("120.00"), started.getHourlyRateSnapshot());
    }

    @Test
    void startsPlayStationMultiSessionWithMultiSnapshot() {
        Session started = startSessionAt(START_TIME, SessionMode.MULTI);

        assertEquals(SessionMode.MULTI, started.getMode());
        assertEquals(new BigDecimal("160.00"), started.getHourlyRateSnapshot());
    }

    @Test
    void startsPingPongSingleAndMultiSessionsWithSelectedSnapshots() {
        CreateStationUseCase createStation = new CreateStationUseCase(
                stationRepository, new StationValidator(), authorization);
        Station singleTable = createStation.execute(
                "Ping Pong 1",
                StationType.PING_PONG,
                new BigDecimal("40.00"),
                new BigDecimal("60.00"));
        Station multiTable = createStation.execute(
                "Ping Pong 2",
                StationType.PING_PONG,
                new BigDecimal("45.00"),
                new BigDecimal("65.00"));

        Session single = startSession(singleTable, START_TIME, SessionMode.SINGLE);
        Session multi = startSession(multiTable, START_TIME, SessionMode.MULTI);

        assertEquals(SessionMode.SINGLE, single.getMode());
        assertEquals(new BigDecimal("40.00"), single.getHourlyRateSnapshot());
        assertEquals(SessionMode.MULTI, multi.getMode());
        assertEquals(new BigDecimal("65.00"), multi.getHourlyRateSnapshot());
    }

    @Test
    void billiardStartsWithoutModeAndUsesOneRate() {
        Station billiard = new CreateStationUseCase(
                stationRepository, new StationValidator(), authorization).execute(
                        "Billiard 1", StationType.BILLIARD, new BigDecimal("80.00"));

        Session started = startSession(billiard, START_TIME, null);

        assertNull(started.getMode());
        assertEquals(new BigDecimal("80.00"), started.getHourlyRateSnapshot());
    }

    @Test
    void modeCapableStationCannotStartWithoutMode() {
        assertThrows(ValidationException.class, () ->
                new StartSessionUseCase(
                        stationRepository,
                        sessionRepository,
                        fixedClock(START_TIME),
                        authorization).execute(station.getId()));
    }

    @Test
    void databaseConstraintAlsoPreventsDuplicateActiveSession() {
        Session started = startSessionAt(START_TIME);
        Session duplicate = new Session(
                null,
                started.getStationId(),
                started.getStationNameSnapshot(),
                started.getStationTypeSnapshot(),
                started.getMode(),
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
    void finishUsesPersistedStartAndSelectedMultiRateSnapshot() {
        Session started = startSessionAt(START_TIME, SessionMode.MULTI);

        new UpdateStationUseCase(
                stationRepository, new StationValidator(), authorization).execute(
                station.getId(),
                station.getName(),
                station.getType(),
                new BigDecimal("200.00"),
                new BigDecimal("240.00"));

        Instant finishTime = START_TIME.plusSeconds(5_400L);
        FinishSessionUseCase finishSession = new FinishSessionUseCase(
                sessionRepository,
                sessionProductRepository,
                new CheckoutService(new PricingService()),
                fixedClock(finishTime),
                authorization);
        Session completed = finishSession.execute(started.getId());

        assertEquals(SessionStatus.COMPLETED, completed.getStatus());
        assertEquals(finishTime, completed.getEndTime());
        assertEquals(SessionMode.MULTI, completed.getMode());
        assertEquals(new BigDecimal("160.00"), completed.getHourlyRateSnapshot());
        assertEquals(new BigDecimal("240.00"), completed.getPlayCost());
        assertEquals(new BigDecimal("0.00"), completed.getProductsCost());
        assertEquals(new BigDecimal("240.00"), completed.getFinalTotal());
        assertTrue(new GetActiveSessionsUseCase(
                sessionRepository, authorization).execute().isEmpty());

        Session reloaded = sessionRepository.findById(completed.getId()).orElseThrow(AssertionError::new);
        assertEquals(SessionMode.MULTI, reloaded.getMode());
        assertEquals(new BigDecimal("240.00"), reloaded.getFinalTotal());

        Instant restartTime = finishTime.plusSeconds(1L);
        Session restarted = startSessionAt(restartTime, SessionMode.MULTI);
        assertFalse(restarted.getId().equals(completed.getId()));
        assertEquals(restartTime, restarted.getStartTime());
        assertEquals(SessionStatus.ACTIVE, restarted.getStatus());
        assertEquals(new BigDecimal("240.00"), restarted.getHourlyRateSnapshot());
    }

    @Test
    void disabledStationCannotStartSession() {
        new SetStationEnabledUseCase(
                stationRepository, authorization).execute(station.getId(), false);

        assertThrows(StationDisabledException.class, () -> startSessionAt(START_TIME));
        assertFalse(sessionRepository.findActiveByStationId(station.getId()).isPresent());
    }

    @Test
    void activeStationCannotBeDisabledBelowTheUiLayer() {
        Session active = startSessionAt(START_TIME);
        SetStationEnabledUseCase setEnabled = new SetStationEnabledUseCase(
                stationRepository, authorization);

        assertThrows(
                StationInUseException.class,
                () -> setEnabled.execute(station.getId(), false));

        assertTrue(stationRepository.findById(station.getId())
                .orElseThrow(AssertionError::new)
                .isEnabled());
        assertEquals(
                active.getId(),
                sessionRepository.findActiveByStationId(station.getId())
                        .orElseThrow(AssertionError::new)
                        .getId());
    }

    private Session startSessionAt(Instant instant) {
        return startSessionAt(instant, SessionMode.SINGLE);
    }

    private Session startSessionAt(Instant instant, SessionMode mode) {
        return startSession(station, instant, mode);
    }

    private Session startSession(Station target, Instant instant, SessionMode mode) {
        return new StartSessionUseCase(
                stationRepository,
                sessionRepository,
                fixedClock(instant),
                authorization)
                .execute(target.getId(), mode);
    }

    private Clock fixedClock(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }
}
