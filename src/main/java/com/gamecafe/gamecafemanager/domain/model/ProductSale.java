package com.gamecafe.gamecafemanager.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * A product-only sale that is not attached to a gaming session. Product name,
 * unit price, and line total are historic snapshots.
 */
public final class ProductSale {

    private final Long id;
    private final long productId;
    private final String productNameSnapshot;
    private final BigDecimal unitPriceSnapshot;
    private final int quantity;
    private final BigDecimal lineTotal;
    private final Instant soldAt;

    public ProductSale(
            Long id,
            long productId,
            String productNameSnapshot,
            BigDecimal unitPriceSnapshot,
            int quantity,
            BigDecimal lineTotal,
            Instant soldAt) {
        this.id = id;
        this.productId = productId;
        this.productNameSnapshot = Objects.requireNonNull(
                productNameSnapshot, "productNameSnapshot");
        this.unitPriceSnapshot = Objects.requireNonNull(
                unitPriceSnapshot, "unitPriceSnapshot");
        this.quantity = quantity;
        this.lineTotal = Objects.requireNonNull(lineTotal, "lineTotal");
        this.soldAt = Objects.requireNonNull(soldAt, "soldAt");
    }

    public Long getId() {
        return id;
    }

    public long getProductId() {
        return productId;
    }

    public String getProductNameSnapshot() {
        return productNameSnapshot;
    }

    public BigDecimal getUnitPriceSnapshot() {
        return unitPriceSnapshot;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public Instant getSoldAt() {
        return soldAt;
    }
}
