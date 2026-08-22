package com.gamecafe.gamecafemanager.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public final class ProductSales {

    private final long productId;
    private final String productName;
    private final long quantitySold;
    private final BigDecimal revenue;

    public ProductSales(
            long productId,
            String productName,
            long quantitySold,
            BigDecimal revenue) {
        this.productId = productId;
        this.productName = Objects.requireNonNull(productName, "productName");
        this.quantitySold = quantitySold;
        this.revenue = Objects.requireNonNull(revenue, "revenue");
    }

    public long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public long getQuantitySold() {
        return quantitySold;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }
}
