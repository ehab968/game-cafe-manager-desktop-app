package com.gamecafe.gamecafemanager.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A sellable inventory item and its current catalog price.
 */
public final class Product {

    private final Long id;
    private final String name;
    private final BigDecimal currentPrice;
    private final int stockQuantity;
    private final boolean enabled;

    public Product(
            Long id,
            String name,
            BigDecimal currentPrice,
            int stockQuantity,
            boolean enabled) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name");
        this.currentPrice = Objects.requireNonNull(currentPrice, "currentPrice");
        this.stockQuantity = stockQuantity;
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
