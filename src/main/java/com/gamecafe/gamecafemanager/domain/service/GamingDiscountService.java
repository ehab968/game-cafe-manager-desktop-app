package com.gamecafe.gamecafemanager.domain.service;

import com.gamecafe.gamecafemanager.domain.model.GamingDiscount;
import com.gamecafe.gamecafemanager.domain.service.pricing.GamingDiscountResult;
import com.gamecafe.gamecafemanager.domain.service.pricing.MonetaryRoundingPolicy;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Applies an allowed percentage discount to gaming cost only.
 */
public final class GamingDiscountService {

    private final MonetaryRoundingPolicy monetaryRoundingPolicy;

    public GamingDiscountService(MonetaryRoundingPolicy monetaryRoundingPolicy) {
        this.monetaryRoundingPolicy = Objects.requireNonNull(
                monetaryRoundingPolicy, "monetaryRoundingPolicy");
    }

    public GamingDiscountResult apply(
            BigDecimal originalGamingCost,
            GamingDiscount discount) {
        Objects.requireNonNull(originalGamingCost, "originalGamingCost");
        Objects.requireNonNull(discount, "discount");
        if (originalGamingCost.signum() < 0) {
            throw new IllegalArgumentException("Original gaming cost cannot be negative");
        }

        BigDecimal original = monetaryRoundingPolicy.round(originalGamingCost);
        BigDecimal discountAmount = monetaryRoundingPolicy.round(
                original.multiply(discount.asRate()));
        BigDecimal discounted = monetaryRoundingPolicy.round(
                original.subtract(discountAmount));

        if (discountAmount.signum() < 0 || discountAmount.compareTo(original) > 0) {
            throw new IllegalArgumentException("Gaming discount amount is outside valid bounds");
        }
        if (discounted.signum() < 0) {
            throw new IllegalArgumentException("Discounted gaming cost cannot be negative");
        }

        return new GamingDiscountResult(
                original, discount, discountAmount, discounted);
    }
}
