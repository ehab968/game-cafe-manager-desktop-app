package com.gamecafe.gamecafemanager.domain.service.pricing;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;

/**
 * Extension point for converting billable duration and a rate snapshot into an
 * unrounded gaming price.
 */
@FunctionalInterface
public interface GamingPricePolicy {

    BigDecimal calculateUnroundedPrice(
            Duration billableDuration,
            BigDecimal hourlyRateSnapshot);

    static GamingPricePolicy proratedHourlyRate() {
        return (billableDuration, hourlyRateSnapshot) -> {
            BigDecimal billableSeconds = BigDecimal.valueOf(billableDuration.getSeconds())
                    .add(BigDecimal.valueOf(billableDuration.getNano(), 9));
            return hourlyRateSnapshot.multiply(billableSeconds)
                    .divide(BigDecimal.valueOf(3_600L), MathContext.DECIMAL128);
        };
    }
}
