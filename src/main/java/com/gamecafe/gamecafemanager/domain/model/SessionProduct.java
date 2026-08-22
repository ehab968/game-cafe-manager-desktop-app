package com.gamecafe.gamecafemanager.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A product purchase attached to a session. Product identity and money values
 * are snapshots so later catalog changes cannot alter session history.
 */
public final class SessionProduct {

    private final Long id;
    private final long sessionId;
    private final long productId;
    private final String productNameSnapshot;
    private final BigDecimal unitPriceSnapshot;
    private final int quantity;
    private final BigDecimal lineTotal;

    public SessionProduct(
            Long id,
            long sessionId,
            long productId,
            String productNameSnapshot,
            BigDecimal unitPriceSnapshot,
            int quantity,
            BigDecimal lineTotal) {
        this.id = id;
        this.sessionId = sessionId;
        this.productId = productId;
        this.productNameSnapshot = Objects.requireNonNull(
                productNameSnapshot, "productNameSnapshot");
        this.unitPriceSnapshot = Objects.requireNonNull(
                unitPriceSnapshot, "unitPriceSnapshot");
        this.quantity = quantity;
        this.lineTotal = Objects.requireNonNull(lineTotal, "lineTotal");
    }

    public Long getId() {
        return id;
    }

    public long getSessionId() {
        return sessionId;
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
}
