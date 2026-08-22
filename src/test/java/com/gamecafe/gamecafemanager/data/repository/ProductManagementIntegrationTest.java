package com.gamecafe.gamecafemanager.data.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.data.sqlite.SQLiteDatabase;
import com.gamecafe.gamecafemanager.domain.model.Product;
import com.gamecafe.gamecafemanager.domain.repository.ProductRepository;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import com.gamecafe.gamecafemanager.domain.service.ProductValidator;
import com.gamecafe.gamecafemanager.domain.usecase.product.CreateProductUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.GetProductsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.SetProductEnabledUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.UpdateProductStockUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.UpdateProductUseCase;
import com.gamecafe.gamecafemanager.support.AuthenticationTestSupport;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProductManagementIntegrationTest {

    @TempDir
    Path temporaryDirectory;

    private CreateProductUseCase createProduct;
    private UpdateProductUseCase updateProduct;
    private GetProductsUseCase getProducts;
    private SetProductEnabledUseCase setProductEnabled;
    private UpdateProductStockUseCase updateProductStock;

    @BeforeEach
    void setUp() {
        SQLiteDatabase database = new SQLiteDatabase(temporaryDirectory.resolve("products.db"));
        database.initialize();
        ProductRepository repository = new SQLiteProductRepository(database);
        ProductValidator validator = new ProductValidator();
        AuthorizationService authorization =
                AuthenticationTestSupport.authenticatedAdmin(database);

        createProduct = new CreateProductUseCase(repository, validator, authorization);
        updateProduct = new UpdateProductUseCase(repository, validator, authorization);
        getProducts = new GetProductsUseCase(repository);
        setProductEnabled = new SetProductEnabledUseCase(repository, authorization);
        updateProductStock = new UpdateProductStockUseCase(repository, validator, authorization);
    }

    @Test
    void createsUpdatesListsChangesStockAndDisablesProduct() {
        Product created = createProduct.execute(
                " Water ", new BigDecimal("12.5"), 10);

        assertTrue(created.getId() > 0);
        assertEquals("Water", created.getName());
        assertEquals(new BigDecimal("12.50"), created.getCurrentPrice());
        assertEquals(10, created.getStockQuantity());
        assertTrue(created.isEnabled());

        Product updated = updateProduct.execute(
                created.getId(), "Mineral Water", new BigDecimal("15.00"), 12);
        assertEquals("Mineral Water", updated.getName());
        assertEquals(new BigDecimal("15.00"), updated.getCurrentPrice());

        updateProductStock.execute(created.getId(), 25);
        setProductEnabled.execute(created.getId(), false);
        List<Product> storedProducts = getProducts.execute();

        assertEquals(1, storedProducts.size());
        assertEquals(25, storedProducts.get(0).getStockQuantity());
        assertFalse(storedProducts.get(0).isEnabled());
    }

    @Test
    void rejectsDuplicateNameIgnoringCase() {
        createProduct.execute("Cola", new BigDecimal("20.00"), 5);

        assertThrows(ValidationException.class, () ->
                createProduct.execute("cola", new BigDecimal("22.00"), 3));
    }
}
