package com.gamecafe.gamecafemanager.domain.repository;

import com.gamecafe.gamecafemanager.domain.model.ProductSale;
import java.time.Instant;
import java.util.List;

/**
 * Persistence protocol for product-only sales outside gaming sessions.
 */
public interface ProductSaleRepository {

    /**
     * Atomically reserves stock and stores historic product and price snapshots.
     */
    ProductSale sell(long productId, int quantity, Instant soldAt);

    List<ProductSale> findAll();
}
