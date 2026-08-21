package com.gamecafe.gamecafemanager.domain.service.pricing;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;

/**
 * Immutable result of one session-price calculation.
 */
public final class PricingResult {

    private final Duration elapsedDuration;
    private final Duration billableDuration;
    private final BigDecimal gamingPrice;

    public PricingResult(
            Duration elapsedDuration,
            Duration billableDuration,
            BigDecimal gamingPrice) {
        this.elapsedDuration = Objects.requireNonNull(elapsedDuration, "elapsedDuration");
        this.billableDuration = Objects.requireNonNull(billableDuration, "billableDuration");
        this.gamingPrice = Objects.requireNonNull(gamingPrice, "gamingPrice");
    }

    public Duration getElapsedDuration() {
        return elapsedDuration;
    }

    public Duration getBillableDuration() {
        return billableDuration;
    }

    public BigDecimal getGamingPrice() {
        return gamingPrice;
    }
}
