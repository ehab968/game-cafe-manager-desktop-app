package com.gamecafe.gamecafemanager.data.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gamecafe.gamecafemanager.data.sqlite.SQLiteDatabase;
import com.gamecafe.gamecafemanager.domain.exception.SessionNotCompletedException;
import com.gamecafe.gamecafemanager.domain.model.Invoice;
import com.gamecafe.gamecafemanager.domain.model.ApplicationSettings;
import com.gamecafe.gamecafemanager.domain.model.Product;
import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.model.Station;
import com.gamecafe.gamecafemanager.domain.model.StationType;
import com.gamecafe.gamecafemanager.domain.repository.ProductRepository;
import com.gamecafe.gamecafemanager.domain.repository.SessionProductRepository;
import com.gamecafe.gamecafemanager.domain.repository.SessionRepository;
import com.gamecafe.gamecafemanager.domain.repository.StationRepository;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import com.gamecafe.gamecafemanager.domain.service.CheckoutService;
import com.gamecafe.gamecafemanager.domain.service.InvoiceService;
import com.gamecafe.gamecafemanager.domain.service.PricingService;
import com.gamecafe.gamecafemanager.domain.service.ProductValidator;
import com.gamecafe.gamecafemanager.domain.service.StationValidator;
import com.gamecafe.gamecafemanager.domain.usecase.invoice.GenerateInvoiceUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.CreateProductUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.UpdateProductUseCase;
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
import java.time.ZoneOffset;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InvoiceIntegrationTest {

    private static final Instant START_TIME = Instant.parse("2026-08-22T12:00:00Z");
    private static final Instant END_TIME = START_TIME.plusSeconds(3_600L);

    @TempDir
    Path temporaryDirectory;

    private StationRepository stationRepository;
    private ProductRepository productRepository;
    private SessionRepository sessionRepository;
    private SessionProductRepository sessionProductRepository;
    private AuthorizationService authorization;
    private Station station;
    private Product product;
    private Session activeSession;
    private GenerateInvoiceUseCase generateInvoice;

    @BeforeEach
    void setUp() {
        SQLiteDatabase database = new SQLiteDatabase(temporaryDirectory.resolve("invoice.db"));
        database.initialize();
        stationRepository = new SQLiteStationRepository(database);
        productRepository = new SQLiteProductRepository(database);
        sessionRepository = new SQLiteSessionRepository(database);
        sessionProductRepository = new SQLiteSessionProductRepository(database);
        authorization = AuthenticationTestSupport.authenticatedAdmin(database);

        station = new CreateStationUseCase(
                stationRepository, new StationValidator(), authorization).execute(
                        "Billiard 1", StationType.BILLIARD, new BigDecimal("80.00"));
        activeSession = new StartSessionUseCase(
                stationRepository,
                sessionRepository,
                fixedClock(START_TIME),
                authorization).execute(station.getId());
        product = new CreateProductUseCase(
                productRepository, new ProductValidator(), authorization).execute(
                        "Chips", new BigDecimal("10.00"), 10);
        new AddProductToSessionUseCase(
                sessionRepository, sessionProductRepository, authorization)
                .execute(activeSession.getId(), product.getId(), 2);
        generateInvoice = new GenerateInvoiceUseCase(
                sessionRepository,
                sessionProductRepository,
                new InvoiceService(() -> new ApplicationSettings(
                        "Pixel Hub", "USD", "Thanks for playing!", null, null)),
                authorization);
    }

    @Test
    void generatesCompletedSessionInvoiceFromHistoricSnapshots() {
        CheckoutService checkoutService = new CheckoutService(new PricingService());
        Session completed = new FinishSessionUseCase(
                sessionRepository,
                sessionProductRepository,
                checkoutService,
                fixedClock(END_TIME),
                authorization).execute(activeSession.getId());

        new UpdateStationUseCase(
                stationRepository, new StationValidator(), authorization).execute(
                station.getId(),
                "Ping-pong 9",
                StationType.PING_PONG,
                new BigDecimal("200.00"));
        new UpdateProductUseCase(
                productRepository, new ProductValidator(), authorization).execute(
                product.getId(), "Premium Chips", new BigDecimal("25.00"), 8);

        Invoice invoice = generateInvoice.execute(completed.getId());

        assertEquals("Pixel Hub", invoice.getCafeName());
        assertEquals("USD", invoice.getCurrencyDisplay());
        assertEquals("Thanks for playing!", invoice.getFooter());
        assertEquals(
                String.format(Locale.ROOT, "INV-%06d", completed.getId()),
                invoice.getInvoiceNumber());
        assertEquals(completed.getId().longValue(), invoice.getSessionId());
        assertEquals("Billiard 1", invoice.getStationName());
        assertEquals(StationType.BILLIARD, invoice.getStationType());
        assertEquals(START_TIME, invoice.getStartTime());
        assertEquals(END_TIME, invoice.getEndTime());
        assertEquals(Duration.ofHours(1L), invoice.getDuration());
        assertEquals(new BigDecimal("80.00"), invoice.getGamingAmount());
        assertEquals(1, invoice.getPurchasedProducts().size());
        assertEquals("Chips", invoice.getPurchasedProducts().get(0).getProductName());
        assertEquals(2, invoice.getPurchasedProducts().get(0).getQuantity());
        assertEquals(new BigDecimal("10.00"),
                invoice.getPurchasedProducts().get(0).getUnitPrice());
        assertEquals(new BigDecimal("20.00"),
                invoice.getPurchasedProducts().get(0).getLineTotal());
        assertEquals(new BigDecimal("20.00"), invoice.getProductsTotal());
        assertEquals(new BigDecimal("100.00"), invoice.getTotal());
    }

    @Test
    void rejectsInvoiceForActiveSession() {
        assertThrows(SessionNotCompletedException.class, () ->
                generateInvoice.execute(activeSession.getId()));
    }

    private Clock fixedClock(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }
}
