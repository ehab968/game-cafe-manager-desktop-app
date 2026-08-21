package com.gamecafe.gamecafemanager.domain.service.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Central extension point for applying monetary precision and rounding.
 */
@FunctionalInterface
public interface MonetaryRoundingPolicy {

    BigDecimal round(BigDecimal amount);

    static MonetaryRoundingPolicy standardCurrency() {
        return amount -> amount.setScale(2, RoundingMode.HALF_UP);
    }
}
