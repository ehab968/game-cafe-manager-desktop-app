package com.gamecafe.gamecafemanager.domain.repository;

import com.gamecafe.gamecafemanager.domain.model.Product;
import java.util.List;
import java.util.Optional;

/**
 * Persistence protocol for products. Implementations belong to the data layer.
 */
public interface ProductRepository {

    Product create(Product product);

    Product update(Product product);

    List<Product> findAll();

    Optional<Product> findById(long id);

    boolean existsByName(String name, Long excludedProductId);

    void setEnabled(long id, boolean enabled);

    void updateStock(long id, int stockQuantity);
}
