package com.gamecafe.gamecafemanager.domain.service;

import com.gamecafe.gamecafemanager.domain.service.pricing.BillableDurationPolicy;
import com.gamecafe.gamecafemanager.domain.service.pricing.GamingPricePolicy;
import com.gamecafe.gamecafemanager.domain.service.pricing.MonetaryRoundingPolicy;
import com.gamecafe.gamecafemanager.domain.service.pricing.PricingResult;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Performs all time-based session pricing independently of presentation and
 * persistence code.
 */
public final class PricingService {

    private final BillableDurationPolicy billableDurationPolicy;
    private final GamingPricePolicy gamingPricePolicy;
    private final MonetaryRoundingPolicy monetaryRoundingPolicy;

    public PricingService() {
        this(
                BillableDurationPolicy.exactElapsedTime(),
                GamingPricePolicy.proratedHourlyRate(),
                MonetaryRoundingPolicy.standardCurrency());
    }

    public PricingService(
            BillableDurationPolicy billableDurationPolicy,
            GamingPricePolicy gamingPricePolicy,
            MonetaryRoundingPolicy monetaryRoundingPolicy) {
        this.billableDurationPolicy = Objects.requireNonNull(
                billableDurationPolicy, "billableDurationPolicy");
        this.gamingPricePolicy = Objects.requireNonNull(gamingPricePolicy, "gamingPricePolicy");
        this.monetaryRoundingPolicy = Objects.requireNonNull(
                monetaryRoundingPolicy, "monetaryRoundingPolicy");
    }

    public PricingResult calculate(
            Instant startTime,
            Instant calculationTime,
            BigDecimal hourlyRateSnapshot) {
        Objects.requireNonNull(startTime, "startTime");
        Objects.requireNonNull(calculationTime, "calculationTime");
        Objects.requireNonNull(hourlyRateSnapshot, "hourlyRateSnapshot");

        if (calculationTime.isBefore(startTime)) {
            throw new IllegalArgumentException(
                    "Session calculation time cannot precede its start time");
        }
        if (hourlyRateSnapshot.signum() < 0) {
            throw new IllegalArgumentException("Hourly rate snapshot cannot be negative");
        }

        Duration elapsedDuration = Duration.between(startTime, calculationTime);
        Duration billableDuration = Objects.requireNonNull(
                billableDurationPolicy.calculateBillableDuration(elapsedDuration),
                "billableDuration");
        if (billableDuration.isNegative()) {
            throw new IllegalArgumentException("Billable duration cannot be negative");
        }

        BigDecimal unroundedPrice = Objects.requireNonNull(
                gamingPricePolicy.calculateUnroundedPrice(
                        billableDuration,
                        hourlyRateSnapshot),
                "unroundedPrice");
        BigDecimal gamingPrice = Objects.requireNonNull(
                monetaryRoundingPolicy.round(unroundedPrice),
                "gamingPrice");

        return new PricingResult(elapsedDuration, billableDuration, gamingPrice);
    }
}
