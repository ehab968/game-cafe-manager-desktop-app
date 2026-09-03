package com.gamecafe.gamecafemanager.data.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.data.sqlite.SQLiteDatabase;
import com.gamecafe.gamecafemanager.domain.exception.InsufficientStockException;
import com.gamecafe.gamecafemanager.domain.exception.ProductDisabledException;
import com.gamecafe.gamecafemanager.domain.model.Product;
import com.gamecafe.gamecafemanager.domain.model.ProductSale;
import com.gamecafe.gamecafemanager.domain.model.Role;
import com.gamecafe.gamecafemanager.domain.repository.ProductRepository;
import com.gamecafe.gamecafemanager.domain.repository.ProductSaleRepository;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import com.gamecafe.gamecafemanager.domain.service.ProductValidator;
import com.gamecafe.gamecafemanager.domain.usecase.product.CreateProductUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.SellProductUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.SetProductEnabledUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.UpdateProductUseCase;
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

class ProductSaleIntegrationTest {

    private static final Instant SOLD_AT = Instant.parse("2026-08-28T18:30:00Z");

    @TempDir
    Path temporaryDirectory;

    private SQLiteDatabase database;
    private ProductRepository productRepository;
    private ProductSaleRepository productSaleRepository;
    private AuthorizationService adminAuthorization;
    private CreateProductUseCase createProduct;
    private SellProductUseCase sellProduct;

    @BeforeEach
    void setUp() {
        database = new SQLiteDatabase(temporaryDirectory.resolve("product-sales.db"));
        database.initialize();
        productRepository = new SQLiteProductRepository(database);
        productSaleRepository = new SQLiteProductSaleRepository(database);
        adminAuthorization = AuthenticationTestSupport.authenticatedAdmin(database);
        createProduct = new CreateProductUseCase(
                productRepository,
                new ProductValidator(),
                adminAuthorization);
        sellProduct = new SellProductUseCase(
                productSaleRepository,
                fixedClock(SOLD_AT),
                adminAuthorization);
    }

    @Test
    void recordsSaleReducesStockAndKeepsHistoricSnapshots() {
        Product product = createProduct.execute(
                "Cola", new BigDecimal("12.50"), 10);

        ProductSale sale = sellProduct.execute(product.getId(), 3);

        assertEquals("Cola", sale.getProductNameSnapshot());
        assertEquals(new BigDecimal("12.50"), sale.getUnitPriceSnapshot());
        assertEquals(3, sale.getQuantity());
        assertEquals(new BigDecimal("37.50"), sale.getLineTotal());
        assertEquals(SOLD_AT, sale.getSoldAt());
        assertEquals(7, storedProduct(product.getId()).getStockQuantity());

        new UpdateProductUseCase(
                productRepository,
                new ProductValidator(),
                adminAuthorization).execute(
                        product.getId(),
                        "Premium Cola",
                        new BigDecimal("20.00"),
                        7);

        List<ProductSale> storedSales = productSaleRepository.findAll();
        assertEquals(1, storedSales.size());
        assertEquals("Cola", storedSales.get(0).getProductNameSnapshot());
        assertEquals(
                new BigDecimal("12.50"),
                storedSales.get(0).getUnitPriceSnapshot());
        assertEquals(new BigDecimal("37.50"), storedSales.get(0).getLineTotal());
    }

    @Test
    void insufficientStockAndDisabledProductsDoNotCreateSales() {
        Product product = createProduct.execute(
                "Water", new BigDecimal("10.00"), 2);

        assertThrows(
                InsufficientStockException.class,
                () -> sellProduct.execute(product.getId(), 3));
        assertEquals(2, storedProduct(product.getId()).getStockQuantity());
        assertTrue(productSaleRepository.findAll().isEmpty());

        new SetProductEnabledUseCase(productRepository, adminAuthorization)
                .execute(product.getId(), false);
        assertThrows(
                ProductDisabledException.class,
                () -> sellProduct.execute(product.getId(), 1));
        assertEquals(2, storedProduct(product.getId()).getStockQuantity());
        assertTrue(productSaleRepository.findAll().isEmpty());
    }

    @Test
    void invalidQuantityIsRejectedBeforePersistence() {
        Product product = createProduct.execute(
                "Juice", new BigDecimal("15.00"), 4);

        assertThrows(
                ValidationException.class,
                () -> sellProduct.execute(product.getId(), 0));

        assertEquals(4, storedProduct(product.getId()).getStockQuantity());
        assertTrue(productSaleRepository.findAll().isEmpty());
    }

    @Test
    void failureAfterStockReservationRollsBackTheWholeSale() {
        Product product = createProduct.execute(
                "Overflow Test",
                new BigDecimal("92233720368547758.07"),
                2);

        assertThrows(
                ArithmeticException.class,
                () -> sellProduct.execute(product.getId(), 2));

        assertEquals(2, storedProduct(product.getId()).getStockQuantity());
        assertTrue(productSaleRepository.findAll().isEmpty());
    }

    @Test
    void cashierCanRecordProductOnlySale() {
        Product product = createProduct.execute(
                "Chips", new BigDecimal("8.00"), 5);
        AuthorizationService cashierAuthorization =
                AuthenticationTestSupport.authenticated(database, Role.CASHIER);
        ProductSale sale = new SellProductUseCase(
                productSaleRepository,
                fixedClock(SOLD_AT),
                cashierAuthorization).execute(product.getId(), 2);

        assertEquals(new BigDecimal("16.00"), sale.getLineTotal());
        assertEquals(3, storedProduct(product.getId()).getStockQuantity());
    }

    private Product storedProduct(long productId) {
        return productRepository.findById(productId)
                .orElseThrow(AssertionError::new);
    }

    private Clock fixedClock(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }
}
