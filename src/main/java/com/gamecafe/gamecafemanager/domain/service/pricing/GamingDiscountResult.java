package com.gamecafe.gamecafemanager.domain.service.pricing;

import com.gamecafe.gamecafemanager.domain.model.GamingDiscount;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable breakdown of a discount applied to gross gaming cost.
 */
public final class GamingDiscountResult {

    private final BigDecimal originalGamingCost;
    private final GamingDiscount discount;
    private final BigDecimal discountAmount;
    private final BigDecimal discountedGamingCost;

    public GamingDiscountResult(
            BigDecimal originalGamingCost,
            GamingDiscount discount,
            BigDecimal discountAmount,
            BigDecimal discountedGamingCost) {
        this.originalGamingCost = Objects.requireNonNull(
                originalGamingCost, "originalGamingCost");
        this.discount = Objects.requireNonNull(discount, "discount");
        this.discountAmount = Objects.requireNonNull(discountAmount, "discountAmount");
        this.discountedGamingCost = Objects.requireNonNull(
                discountedGamingCost, "discountedGamingCost");
    }

    public BigDecimal getOriginalGamingCost() {
        return originalGamingCost;
    }

    public GamingDiscount getDiscount() {
        return discount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getDiscountedGamingCost() {
        return discountedGamingCost;
    }
}
