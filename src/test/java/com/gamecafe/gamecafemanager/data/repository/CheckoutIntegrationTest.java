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
import com.gamecafe.gamecafemanager.domain.model.GamingDiscount;
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
    private SessionProductRepository sessionProductRepository;
    private StationRepository stationRepository;
    private ProductRepository productRepository;
    private AuthorizationService authorization;
    private PrepareCheckoutUseCase prepareCheckout;
    private FinishSessionUseCase finishSession;

    @BeforeEach
    void setUp() {
        SQLiteDatabase database = new SQLiteDatabase(temporaryDirectory.resolve("checkout.db"));
        database.initialize();
        stationRepository = new SQLiteStationRepository(database);
        productRepository = new SQLiteProductRepository(database);
        sessionRepository = new SQLiteSessionRepository(database);
        sessionProductRepository =
                new SQLiteSessionProductRepository(database);
        authorization =
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
        assertEquals(GamingDiscount.NONE, summary.getGamingDiscount());
        assertEquals(new BigDecimal("0.00"), summary.getGamingDiscountAmount());
        assertEquals(new BigDecimal("210.00"), summary.getDiscountedGamingCost());
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
    void twentyPercentDiscountAffectsOnlyGamingAndPersistsEverySnapshot() {
        CheckoutSummary summary = prepareCheckout.execute(
                activeSession.getId(), GamingDiscount.TWENTY_PERCENT);

        assertEquals(new BigDecimal("210.00"), summary.getGamingCost());
        assertEquals(GamingDiscount.TWENTY_PERCENT, summary.getGamingDiscount());
        assertEquals(new BigDecimal("42.00"), summary.getGamingDiscountAmount());
        assertEquals(new BigDecimal("168.00"), summary.getDiscountedGamingCost());
        assertEquals(new BigDecimal("25.00"), summary.getProductsTotal());
        assertEquals(new BigDecimal("193.00"), summary.getFinalTotal());

        Session completed = finishSession.execute(
                summary.getSessionId(),
                summary.getEndTime(),
                summary.getGamingDiscount());
        Session persisted = sessionRepository.findById(completed.getId())
                .orElseThrow(AssertionError::new);

        assertEquals(new BigDecimal("140.00"), persisted.getHourlyRateSnapshot());
        assertEquals(new BigDecimal("210.00"), persisted.getPlayCost());
        assertEquals(GamingDiscount.TWENTY_PERCENT, persisted.getGamingDiscount());
        assertEquals(new BigDecimal("42.00"), persisted.getGamingDiscountAmount());
        assertEquals(new BigDecimal("168.00"), persisted.getDiscountedPlayCost());
        assertEquals(new BigDecimal("25.00"), persisted.getProductsCost());
        assertEquals(new BigDecimal("193.00"), persisted.getFinalTotal());
        Product persistedProduct = productRepository.findById(1L)
                .orElseThrow(AssertionError::new);
        assertEquals(new BigDecimal("12.50"), persistedProduct.getCurrentPrice());
        assertEquals(8, persistedProduct.getStockQuantity());
    }

    @Test
    void discountsWorkForSingleMultiBilliardAndPingPongSessions() {
        assertDiscountedOneHourSession(
                "PS Single",
                StationType.PLAYSTATION,
                SessionMode.SINGLE,
                "60.00",
                "80.00",
                "48.00");
        assertDiscountedOneHourSession(
                "PS Multi",
                StationType.PLAYSTATION,
                SessionMode.MULTI,
                "60.00",
                "80.00",
                "64.00");
        assertDiscountedOneHourSession(
                "Ping Pong Single",
                StationType.PING_PONG,
                SessionMode.SINGLE,
                "40.00",
                "60.00",
                "32.00");
        assertDiscountedOneHourSession(
                "Ping Pong Multi",
                StationType.PING_PONG,
                SessionMode.MULTI,
                "40.00",
                "60.00",
                "48.00");
        assertDiscountedOneHourSession(
                "Billiard Discount",
                StationType.BILLIARD,
                null,
                "50.00",
                null,
                "40.00");
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

    private void assertDiscountedOneHourSession(
            String name,
            StationType type,
            SessionMode mode,
            String singleRate,
            String multiRate,
            String expectedNet) {
        CreateStationUseCase createStation = new CreateStationUseCase(
                stationRepository, new StationValidator(), authorization);
        Station created = type.supportsSessionModes()
                ? createStation.execute(
                        name,
                        type,
                        new BigDecimal(singleRate),
                        new BigDecimal(multiRate))
                : createStation.execute(name, type, new BigDecimal(singleRate));
        Session started = new StartSessionUseCase(
                stationRepository,
                sessionRepository,
                fixedClock(START_TIME),
                authorization).execute(created.getId(), mode);

        Session completed = finishSession.execute(
                started.getId(),
                START_TIME.plus(Duration.ofHours(1L)),
                GamingDiscount.TWENTY_PERCENT);

        assertEquals(GamingDiscount.TWENTY_PERCENT, completed.getGamingDiscount());
        assertEquals(new BigDecimal(expectedNet), completed.getDiscountedPlayCost());
        assertEquals(new BigDecimal(expectedNet), completed.getFinalTotal());
    }
}
