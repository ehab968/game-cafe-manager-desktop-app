package com.gamecafe.gamecafemanager.data.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.data.sqlite.SQLiteDatabase;
import com.gamecafe.gamecafemanager.domain.exception.InsufficientStockException;
import com.gamecafe.gamecafemanager.domain.exception.SessionNotActiveException;
import com.gamecafe.gamecafemanager.domain.model.Product;
import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.model.SessionProduct;
import com.gamecafe.gamecafemanager.domain.model.Station;
import com.gamecafe.gamecafemanager.domain.model.StationType;
import com.gamecafe.gamecafemanager.domain.repository.ProductRepository;
import com.gamecafe.gamecafemanager.domain.repository.SessionProductRepository;
import com.gamecafe.gamecafemanager.domain.repository.SessionRepository;
import com.gamecafe.gamecafemanager.domain.repository.StationRepository;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import com.gamecafe.gamecafemanager.domain.service.PricingService;
import com.gamecafe.gamecafemanager.domain.service.CheckoutService;
import com.gamecafe.gamecafemanager.domain.service.ProductValidator;
import com.gamecafe.gamecafemanager.domain.service.StationValidator;
import com.gamecafe.gamecafemanager.domain.usecase.product.CreateProductUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.UpdateProductUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.session.FinishSessionUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.session.StartSessionUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.sessionproduct.AddProductToSessionUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.sessionproduct.GetSessionProductsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.CreateStationUseCase;
import com.gamecafe.gamecafemanager.support.AuthenticationTestSupport;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SessionProductIntegrationTest {

    private static final Instant START_TIME = Instant.parse("2026-08-21T12:00:00Z");

    @TempDir
    Path temporaryDirectory;

    private ProductRepository productRepository;
    private SessionRepository sessionRepository;
    private SessionProductRepository sessionProductRepository;
    private CreateProductUseCase createProduct;
    private UpdateProductUseCase updateProduct;
    private AddProductToSessionUseCase addProduct;
    private GetSessionProductsUseCase getSessionProducts;
    private AuthorizationService authorization;
    private Session activeSession;

    @BeforeEach
    void setUp() {
        SQLiteDatabase database = new SQLiteDatabase(
                temporaryDirectory.resolve("session-products.db"));
        database.initialize();
        StationRepository stationRepository = new SQLiteStationRepository(database);
        productRepository = new SQLiteProductRepository(database);
        sessionRepository = new SQLiteSessionRepository(database);
        sessionProductRepository = new SQLiteSessionProductRepository(database);
        authorization = AuthenticationTestSupport.authenticatedAdmin(database);

        ProductValidator productValidator = new ProductValidator();
        createProduct = new CreateProductUseCase(
                productRepository, productValidator, authorization);
        updateProduct = new UpdateProductUseCase(
                productRepository, productValidator, authorization);
        addProduct = new AddProductToSessionUseCase(
                sessionRepository, sessionProductRepository, authorization);
        getSessionProducts = new GetSessionProductsUseCase(sessionProductRepository);

        Station station = new CreateStationUseCase(
                stationRepository, new StationValidator(), authorization).execute(
                        "Room 1", StationType.PLAYSTATION, new BigDecimal("120.00"));
        activeSession = new StartSessionUseCase(
                stationRepository,
                sessionRepository,
                fixedClock(START_TIME),
                authorization).execute(station.getId());
    }

    @Test
    void addsItemsUpdatesTotalsAndKeepsHistoricSnapshots() {
        Product product = createProduct.execute("Cola", new BigDecimal("12.50"), 10);

        SessionProduct firstItem = addProduct.execute(activeSession.getId(), product.getId(), 3);
        assertEquals("Cola", firstItem.getProductNameSnapshot());
        assertEquals(new BigDecimal("12.50"), firstItem.getUnitPriceSnapshot());
        assertEquals(3, firstItem.getQuantity());
        assertEquals(new BigDecimal("37.50"), firstItem.getLineTotal());
        assertEquals(7, storedProduct(product.getId()).getStockQuantity());

        updateProduct.execute(
                product.getId(), "Cola New", new BigDecimal("20.00"), 7);
        addProduct.execute(activeSession.getId(), product.getId(), 1);

        List<SessionProduct> items = getSessionProducts.execute(activeSession.getId());
        assertEquals(2, items.size());
        assertEquals("Cola", items.get(0).getProductNameSnapshot());
        assertEquals(new BigDecimal("12.50"), items.get(0).getUnitPriceSnapshot());
        assertEquals(new BigDecimal("37.50"), items.get(0).getLineTotal());
        assertEquals("Cola New", items.get(1).getProductNameSnapshot());
        assertEquals(new BigDecimal("20.00"), items.get(1).getUnitPriceSnapshot());

        Session withProducts = storedSession();
        assertEquals(new BigDecimal("57.50"), withProducts.getProductsCost());
        assertEquals(new BigDecimal("57.50"), withProducts.getFinalTotal());
        assertEquals(6, storedProduct(product.getId()).getStockQuantity());

        Session completed = new FinishSessionUseCase(
                sessionRepository,
                sessionProductRepository,
                new CheckoutService(new PricingService()),
                fixedClock(START_TIME.plusSeconds(3_600L)),
                authorization)
                .execute(activeSession.getId());
        assertEquals(new BigDecimal("120.00"), completed.getPlayCost());
        assertEquals(new BigDecimal("57.50"), completed.getProductsCost());
        assertEquals(new BigDecimal("177.50"), completed.getFinalTotal());
    }

    @Test
    void insufficientStockDoesNotChangeInventoryItemsOrTotals() {
        Product product = createProduct.execute("Water", new BigDecimal("10.00"), 2);

        assertThrows(InsufficientStockException.class, () ->
                addProduct.execute(activeSession.getId(), product.getId(), 3));

        assertEquals(2, storedProduct(product.getId()).getStockQuantity());
        assertTrue(getSessionProducts.execute(activeSession.getId()).isEmpty());
        assertEquals(new BigDecimal("0.00"), storedSession().getProductsCost());
        assertEquals(new BigDecimal("0.00"), storedSession().getFinalTotal());
    }

    @Test
    void exactStockCanReachZeroAndRejectedExtraPurchasePreservesSuccessfulTotals() {
        Product product = createProduct.execute("Snack", new BigDecimal("4.25"), 3);

        SessionProduct purchased = addProduct.execute(
                activeSession.getId(), product.getId(), 3);
        assertEquals(new BigDecimal("12.75"), purchased.getLineTotal());
        assertEquals(0, storedProduct(product.getId()).getStockQuantity());

        assertThrows(InsufficientStockException.class, () ->
                addProduct.execute(activeSession.getId(), product.getId(), 1));

        List<SessionProduct> storedItems = getSessionProducts.execute(activeSession.getId());
        assertEquals(1, storedItems.size());
        assertEquals(purchased.getId(), storedItems.get(0).getId());
        assertEquals(0, storedProduct(product.getId()).getStockQuantity());
        assertEquals(new BigDecimal("12.75"), storedSession().getProductsCost());
        assertEquals(new BigDecimal("12.75"), storedSession().getFinalTotal());
    }

    @Test
    void transactionRestoresReservedStockWhenSessionIsNotActive() {
        Product product = createProduct.execute("Chips", new BigDecimal("8.00"), 4);

        assertThrows(SessionNotActiveException.class, () ->
                sessionProductRepository.addToActiveSession(999_999L, product.getId(), 2));

        assertEquals(4, storedProduct(product.getId()).getStockQuantity());
        assertTrue(getSessionProducts.execute(999_999L).isEmpty());
    }

    @Test
    void rejectsNonPositiveQuantityBeforePersistence() {
        Product product = createProduct.execute("Juice", new BigDecimal("18.00"), 4);

        assertThrows(ValidationException.class, () ->
                addProduct.execute(activeSession.getId(), product.getId(), 0));

        assertEquals(4, storedProduct(product.getId()).getStockQuantity());
        assertTrue(getSessionProducts.execute(activeSession.getId()).isEmpty());
    }

    private Product storedProduct(long productId) {
        return productRepository.findById(productId).orElseThrow(AssertionError::new);
    }

    private Session storedSession() {
        return sessionRepository.findById(activeSession.getId()).orElseThrow(AssertionError::new);
    }

    private Clock fixedClock(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }
}
