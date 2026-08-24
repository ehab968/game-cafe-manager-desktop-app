package com.gamecafe.gamecafemanager.data.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gamecafe.gamecafemanager.data.sqlite.SQLiteDatabase;
import com.gamecafe.gamecafemanager.domain.exception.DuplicateCheckoutException;
import com.gamecafe.gamecafemanager.domain.exception.SessionNotActiveException;
import com.gamecafe.gamecafemanager.domain.model.CheckoutSummary;
import com.gamecafe.gamecafemanager.domain.model.Product;
import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.model.SessionMode;
import com.gamecafe.gamecafemanager.domain.model.SessionStatus;
import com.gamecafe.gamecafemanager.domain.model.Station;
import com.gamecafe.gamecafemanager.domain.model.StationType;
import com.gamecafe.gamecafemanager.domain.repository.ProductRepository;
import com.gamecafe.gamecafemanager.domain.repository.SessionProductRepository;
import com.gamecafe.gamecafemanager.domain.repository.SessionRepository;
import com.gamecafe.gamecafemanager.domain.repository.StationRepository;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import com.gamecafe.gamecafemanager.domain.service.CheckoutService;
import com.gamecafe.gamecafemanager.domain.service.PricingService;
import com.gamecafe.gamecafemanager.domain.service.ProductValidator;
import com.gamecafe.gamecafemanager.domain.service.StationValidator;
import com.gamecafe.gamecafemanager.domain.usecase.product.CreateProductUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.session.FinishSessionUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.session.PrepareCheckoutUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.session.StartSessionUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.sessionproduct.AddProductToSessionUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.CreateStationUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.UpdateStationUseCase;
import com.gamecafe.gamecafemanager.support.AuthenticationTestSupport;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CheckoutIntegrationTest {

    private static final Instant START_TIME = Instant.parse("2026-08-21T12:00:00Z");
    private static final Instant CHECKOUT_TIME = START_TIME.plusSeconds(5_400L);

    @TempDir
    Path temporaryDirectory;

    private Station station;
    private Session activeSession;
    private SessionRepository sessionRepository;
    private PrepareCheckoutUseCase prepareCheckout;
    private FinishSessionUseCase finishSession;

    @BeforeEach
    void setUp() {
        SQLiteDatabase database = new SQLiteDatabase(temporaryDirectory.resolve("checkout.db"));
        database.initialize();
        StationRepository stationRepository = new SQLiteStationRepository(database);
        ProductRepository productRepository = new SQLiteProductRepository(database);
        sessionRepository = new SQLiteSessionRepository(database);
        SessionProductRepository sessionProductRepository =
                new SQLiteSessionProductRepository(database);
        AuthorizationService authorization =
                AuthenticationTestSupport.authenticatedAdmin(database);

        station = new CreateStationUseCase(
                stationRepository, new StationValidator(), authorization).execute(
                        "PlayStation Room 1",
                        StationType.PLAYSTATION,
                        new BigDecimal("100.00"),
                        new BigDecimal("140.00"));
        activeSession = new StartSessionUseCase(
                stationRepository,
                sessionRepository,
                fixedClock(START_TIME),
                authorization).execute(station.getId(), SessionMode.MULTI);
        new UpdateStationUseCase(
                stationRepository, new StationValidator(), authorization).execute(
                        station.getId(),
                        station.getName(),
                        station.getType(),
                        new BigDecimal("200.00"),
                        new BigDecimal("260.00"));
        Product product = new CreateProductUseCase(
                productRepository, new ProductValidator(), authorization).execute(
                        "Water", new BigDecimal("12.50"), 10);
        new AddProductToSessionUseCase(
                sessionRepository, sessionProductRepository, authorization)
                .execute(activeSession.getId(), product.getId(), 2);

        CheckoutService checkoutService = new CheckoutService(new PricingService());
        prepareCheckout = new PrepareCheckoutUseCase(
                sessionRepository,
                sessionProductRepository,
                checkoutService,
                fixedClock(CHECKOUT_TIME),
                authorization);
        finishSession = new FinishSessionUseCase(
                sessionRepository,
                sessionProductRepository,
                checkoutService,
                fixedClock(CHECKOUT_TIME),
                authorization);
    }

    @Test
    void previewContainsCompleteSummaryWithoutCompletingSession() {
        CheckoutSummary summary = prepareCheckout.execute(activeSession.getId());

        assertEquals(activeSession.getId().longValue(), summary.getSessionId());
        assertEquals(station.getName(), summary.getStationName());
        assertEquals(StationType.PLAYSTATION, summary.getStationType());
        assertEquals(SessionMode.MULTI, summary.getMode());
        assertEquals(new BigDecimal("140.00"), summary.getHourlyRateSnapshot());
        assertEquals(START_TIME, summary.getStartTime());
        assertEquals(CHECKOUT_TIME, summary.getEndTime());
        assertEquals(Duration.ofMinutes(90L), summary.getDuration());
        assertEquals(new BigDecimal("210.00"), summary.getGamingCost());
        assertEquals(1, summary.getPurchasedProducts().size());
        assertEquals("Water", summary.getPurchasedProducts().get(0).getProductNameSnapshot());
        assertEquals(new BigDecimal("12.50"),
                summary.getPurchasedProducts().get(0).getUnitPriceSnapshot());
        assertEquals(2, summary.getPurchasedProducts().get(0).getQuantity());
        assertEquals(new BigDecimal("25.00"), summary.getProductsTotal());
        assertEquals(new BigDecimal("235.00"), summary.getFinalTotal());

        Session stillActive = sessionRepository.findById(activeSession.getId())
                .orElseThrow(AssertionError::new);
        assertEquals(SessionStatus.ACTIVE, stillActive.getStatus());
        assertTrue(sessionRepository.findActiveByStationId(station.getId()).isPresent());
    }

    @Test
    void confirmationCompletesSessionStoresTotalsAndMakesStationAvailable() {
        CheckoutSummary summary = prepareCheckout.execute(activeSession.getId());
        Session completed = finishSession.execute(summary.getSessionId(), summary.getEndTime());

        assertEquals(SessionStatus.COMPLETED, completed.getStatus());
        assertEquals(CHECKOUT_TIME, completed.getEndTime());
        assertEquals(new BigDecimal("210.00"), completed.getPlayCost());
        assertEquals(new BigDecimal("25.00"), completed.getProductsCost());
        assertEquals(new BigDecimal("235.00"), completed.getFinalTotal());
        assertFalse(sessionRepository.findActiveByStationId(station.getId()).isPresent());

        Session persisted = sessionRepository.findById(activeSession.getId())
                .orElseThrow(AssertionError::new);
        assertNotNull(persisted.getEndTime());
        assertEquals(SessionStatus.COMPLETED, persisted.getStatus());
        assertEquals(new BigDecimal("235.00"), persisted.getFinalTotal());
    }

    @Test
    void rejectsCheckoutPerformedTwice() {
        CheckoutSummary summary = prepareCheckout.execute(activeSession.getId());
        finishSession.execute(summary.getSessionId(), summary.getEndTime());

        assertThrows(DuplicateCheckoutException.class, () ->
                finishSession.execute(summary.getSessionId(), summary.getEndTime()));
    }

    @Test
    void repositoryRecomputesPersistedProductTotalsAndClaimsCheckoutOnlyOnce() {
        Session untrustedCompletion = new Session(
                activeSession.getId(),
                activeSession.getStationId(),
                activeSession.getStationNameSnapshot(),
                activeSession.getStationTypeSnapshot(),
                activeSession.getMode(),
                activeSession.getStartTime(),
                CHECKOUT_TIME,
                SessionStatus.COMPLETED,
                activeSession.getHourlyRateSnapshot(),
                new BigDecimal("210.00"),
                new BigDecimal("999.00"),
                new BigDecimal("1209.00"));

        Session completed = sessionRepository.finish(untrustedCompletion);

        assertEquals(new BigDecimal("210.00"), completed.getPlayCost());
        assertEquals(new BigDecimal("25.00"), completed.getProductsCost());
        assertEquals(new BigDecimal("235.00"), completed.getFinalTotal());
        assertThrows(
                DuplicateCheckoutException.class,
                () -> sessionRepository.finish(untrustedCompletion));
    }

    @Test
    void rejectsFinishingCancelledSessionAsInactive() {
        Session cancelled = sessionRepository.create(new Session(
                null,
                station.getId(),
                station.getName(),
                station.getType(),
                SessionMode.MULTI,
                START_TIME.minusSeconds(3_600L),
                START_TIME.minusSeconds(1_800L),
                SessionStatus.CANCELLED,
                activeSession.getHourlyRateSnapshot(),
                new BigDecimal("0.00"),
                new BigDecimal("0.00"),
                new BigDecimal("0.00")));

        assertThrows(SessionNotActiveException.class, () ->
                finishSession.execute(cancelled.getId(), CHECKOUT_TIME));
    }

    private Clock fixedClock(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }
}
