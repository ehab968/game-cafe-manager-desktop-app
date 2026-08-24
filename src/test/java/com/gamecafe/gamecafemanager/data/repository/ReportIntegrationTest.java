package com.gamecafe.gamecafemanager.data.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.data.sqlite.SQLiteDatabase;
import com.gamecafe.gamecafemanager.domain.exception.AuthorizationException;
import com.gamecafe.gamecafemanager.domain.model.CompletedSessionsReport;
import com.gamecafe.gamecafemanager.domain.model.Product;
import com.gamecafe.gamecafemanager.domain.model.Role;
import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.model.SessionMode;
import com.gamecafe.gamecafemanager.domain.model.Station;
import com.gamecafe.gamecafemanager.domain.model.StationType;
import com.gamecafe.gamecafemanager.domain.repository.ProductRepository;
import com.gamecafe.gamecafemanager.domain.repository.ReportRepository;
import com.gamecafe.gamecafemanager.domain.repository.SessionProductRepository;
import com.gamecafe.gamecafemanager.domain.repository.SessionRepository;
import com.gamecafe.gamecafemanager.domain.repository.StationRepository;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import com.gamecafe.gamecafemanager.domain.service.CheckoutService;
import com.gamecafe.gamecafemanager.domain.service.PricingService;
import com.gamecafe.gamecafemanager.domain.service.ProductValidator;
import com.gamecafe.gamecafemanager.domain.service.StationValidator;
import com.gamecafe.gamecafemanager.domain.usecase.product.CreateProductUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.UpdateProductUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.report.GetReportUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.session.FinishSessionUseCase;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReportIntegrationTest {

    private static final LocalDate FIRST_DAY = LocalDate.parse("2026-08-20");
    private static final LocalDate SECOND_DAY = LocalDate.parse("2026-08-21");
    private static final Instant FIRST_START = Instant.parse("2026-08-20T10:00:00Z");
    private static final Instant SECOND_START = Instant.parse("2026-08-20T14:00:00Z");
    private static final Instant THIRD_START = Instant.parse("2026-08-21T12:00:00Z");

    @TempDir
    Path temporaryDirectory;

    private SQLiteDatabase database;
    private StationRepository stationRepository;
    private ProductRepository productRepository;
    private SessionRepository sessionRepository;
    private SessionProductRepository sessionProductRepository;
    private AuthorizationService authorization;
    private GetReportUseCase getReport;

    @BeforeEach
    void setUp() {
        database = new SQLiteDatabase(temporaryDirectory.resolve("reports.db"));
        database.initialize();
        stationRepository = new SQLiteStationRepository(database);
        productRepository = new SQLiteProductRepository(database);
        sessionRepository = new SQLiteSessionRepository(database);
        sessionProductRepository = new SQLiteSessionProductRepository(database);
        ReportRepository reportRepository = new SQLiteReportRepository(database);
        authorization = AuthenticationTestSupport.authenticatedAdmin(database);

        Station roomA = createStation(
                "Room A", StationType.PLAYSTATION, new BigDecimal("120.00"));
        Station tableB = createStation(
                "Table B", StationType.BILLIARD, new BigDecimal("80.00"));
        Product cola = createProduct("Cola", new BigDecimal("10.00"));
        Product chips = createProduct("Chips", new BigDecimal("5.00"));

        completeSession(roomA, FIRST_START, Duration.ofHours(1L), cola, 2);
        completeSession(roomA, SECOND_START, Duration.ofMinutes(30L), chips, 1);
        completeSession(tableB, THIRD_START, Duration.ofMinutes(90L), cola, 3);

        new UpdateStationUseCase(
                stationRepository, new StationValidator(), authorization).execute(
                        roomA.getId(),
                        "Renamed Room",
                        roomA.getType(),
                        roomA.getSingleHourlyRate(),
                        roomA.getMultiHourlyRate());
        new UpdateProductUseCase(
                productRepository, new ProductValidator(), authorization).execute(
                        cola.getId(), "Premium Cola", new BigDecimal("25.00"), 95);

        new StartSessionUseCase(
                stationRepository,
                sessionRepository,
                fixedClock(Instant.parse("2026-08-21T16:00:00Z")),
                authorization).execute(roomA.getId(), SessionMode.SINGLE);

        getReport = new GetReportUseCase(
                reportRepository,
                authorization,
                fixedClock(Instant.parse("2026-08-21T18:00:00Z")),
                ZoneOffset.UTC);
    }

    @Test
    void reportsTodayDailyAndInclusiveDateRangeFromCompletedSessions() {
        CompletedSessionsReport today = getReport.executeToday();
        assertSummary(today, 1L, "120.00", "30.00", "150.00", Duration.ofMinutes(90L));
        assertEquals(SECOND_DAY, today.getPeriod().getStartDate());

        CompletedSessionsReport daily = getReport.executeDay(FIRST_DAY);
        assertSummary(daily, 2L, "180.00", "25.00", "205.00", Duration.ofMinutes(45L));

        CompletedSessionsReport range = getReport.execute(FIRST_DAY, SECOND_DAY);
        assertSummary(range, 3L, "300.00", "55.00", "355.00", Duration.ofHours(1L));
        assertEquals(FIRST_DAY, range.getPeriod().getStartDate());
        assertEquals(SECOND_DAY, range.getPeriod().getEndDate());
    }

    @Test
    void ranksStationsAndProductsUsingHistoricSnapshots() {
        CompletedSessionsReport report = getReport.execute(FIRST_DAY, SECOND_DAY);

        assertEquals(2, report.getStationUsage().size());
        assertEquals("Room A", report.getStationUsage().get(0).getStationName());
        assertEquals(2L, report.getStationUsage().get(0).getCompletedSessions());
        assertEquals(Duration.ofMinutes(90L),
                report.getStationUsage().get(0).getTotalDuration());
        assertEquals("Table B", report.getStationUsage().get(1).getStationName());

        assertEquals(2, report.getProductSales().size());
        assertEquals("Cola", report.getProductSales().get(0).getProductName());
        assertEquals(5L, report.getProductSales().get(0).getQuantitySold());
        assertEquals(new BigDecimal("50.00"), report.getProductSales().get(0).getRevenue());
        assertEquals("Chips", report.getProductSales().get(1).getProductName());
        assertEquals(1L, report.getProductSales().get(1).getQuantitySold());
    }

    @Test
    void returnsEmptyReportValidatesRangeAndDeniesCashier() {
        CompletedSessionsReport empty = getReport.executeDay(LocalDate.parse("2026-08-22"));
        assertSummary(empty, 0L, "0.00", "0.00", "0.00", Duration.ZERO);
        assertTrue(empty.getStationUsage().isEmpty());
        assertTrue(empty.getProductSales().isEmpty());

        assertThrows(ValidationException.class, () ->
                getReport.execute(SECOND_DAY, FIRST_DAY));

        SQLiteDatabase cashierDatabase = new SQLiteDatabase(
                temporaryDirectory.resolve("cashier-reports.db"));
        cashierDatabase.initialize();
        AuthorizationService cashierAuthorization =
                AuthenticationTestSupport.authenticated(cashierDatabase, Role.CASHIER);
        GetReportUseCase cashierReport = new GetReportUseCase(
                new SQLiteReportRepository(cashierDatabase),
                cashierAuthorization,
                fixedClock(Instant.parse("2026-08-21T18:00:00Z")),
                ZoneOffset.UTC);
        assertThrows(AuthorizationException.class, cashierReport::executeToday);
    }

    private Station createStation(String name, StationType type, BigDecimal rate) {
        CreateStationUseCase createStation = new CreateStationUseCase(
                stationRepository, new StationValidator(), authorization);
        return type.supportsSessionModes()
                ? createStation.execute(name, type, rate, rate)
                : createStation.execute(name, type, rate);
    }

    private Product createProduct(String name, BigDecimal price) {
        return new CreateProductUseCase(
                productRepository, new ProductValidator(), authorization)
                .execute(name, price, 100);
    }

    private Session completeSession(
            Station station,
            Instant start,
            Duration duration,
            Product product,
            int quantity) {
        Session active = new StartSessionUseCase(
                stationRepository,
                sessionRepository,
                fixedClock(start),
                authorization).execute(
                        station.getId(),
                        station.getType().supportsSessionModes() ? SessionMode.SINGLE : null);
        new AddProductToSessionUseCase(
                sessionRepository, sessionProductRepository, authorization)
                .execute(active.getId(), product.getId(), quantity);
        Instant end = start.plus(duration);
        return new FinishSessionUseCase(
                sessionRepository,
                sessionProductRepository,
                new CheckoutService(new PricingService()),
                fixedClock(end),
                authorization).execute(active.getId(), end);
    }

    private void assertSummary(
            CompletedSessionsReport report,
            long sessions,
            String gamingRevenue,
            String productsRevenue,
            String totalRevenue,
            Duration averageDuration) {
        assertEquals(sessions, report.getCompletedSessionsCount());
        assertEquals(new BigDecimal(gamingRevenue), report.getGamingRevenue());
        assertEquals(new BigDecimal(productsRevenue), report.getProductsRevenue());
        assertEquals(new BigDecimal(totalRevenue), report.getTotalRevenue());
        assertEquals(averageDuration, report.getAverageSessionDuration());
    }

    private Clock fixedClock(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }
}
