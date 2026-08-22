package com.gamecafe.gamecafemanager.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable product line rendered on an invoice.
 */
public final class InvoiceItem {

    private final long productId;
    private final String productName;
    private final BigDecimal unitPrice;
    private final int quantity;
    private final BigDecimal lineTotal;

    public InvoiceItem(
            long productId,
            String productName,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal) {
        this.productId = productId;
        this.productName = Objects.requireNonNull(productName, "productName");
        this.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice");
        this.quantity = quantity;
        this.lineTotal = Objects.requireNonNull(lineTotal, "lineTotal");
    }

    public long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}
