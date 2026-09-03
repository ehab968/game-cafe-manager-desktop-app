package com.gamecafe.gamecafemanager.domain.model;

import java.math.BigDecimal;

/**
 * Supported cashier-selectable discounts for gaming time only.
 */
public enum GamingDiscount {

    NONE(0, "No Discount"),
    TEN_PERCENT(10, "10%"),
    TWENTY_PERCENT(20, "20%"),
    FIFTY_PERCENT(50, "50%");

    private final int percentage;
    private final String displayName;

    GamingDiscount(int percentage, String displayName) {
        this.percentage = percentage;
        this.displayName = displayName;
    }

    public int getPercentage() {
        return percentage;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BigDecimal asRate() {
        return BigDecimal.valueOf(percentage).movePointLeft(2);
    }

    public boolean isApplied() {
        return percentage > 0;
    }

    public static GamingDiscount fromPercentage(int percentage) {
        for (GamingDiscount discount : values()) {
            if (discount.percentage == percentage) {
                return discount;
            }
        }
        throw new IllegalArgumentException(
                "Unsupported gaming discount percentage: " + percentage);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
