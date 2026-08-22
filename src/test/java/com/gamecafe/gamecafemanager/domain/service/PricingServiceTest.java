package com.gamecafe.gamecafemanager.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gamecafe.gamecafemanager.domain.model.ApplicationSettings;
import com.gamecafe.gamecafemanager.domain.service.pricing.BillableDurationPolicy;
import com.gamecafe.gamecafemanager.domain.service.pricing.GamingPricePolicy;
import com.gamecafe.gamecafemanager.domain.service.pricing.MonetaryRoundingPolicy;
import com.gamecafe.gamecafemanager.domain.service.pricing.PricingResult;
import com.gamecafe.gamecafemanager.domain.service.pricing.SettingsBillableDurationPolicy;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PricingServiceTest {

    private final PricingService pricingService = new PricingService();
    private final Instant start = Instant.parse("2026-08-21T12:00:00Z");
    private final BigDecimal hourlyRate = new BigDecimal("120.00");

    @Test
    void calculatesThirtyMinutes() {
        assertPrice(Duration.ofMinutes(30L), "60.00");
    }

    @Test
    void calculatesSixtyMinutes() {
        assertPrice(Duration.ofMinutes(60L), "120.00");
    }

    @Test
    void calculatesNinetyMinutes() {
        assertPrice(Duration.ofMinutes(90L), "180.00");
    }

    @Test
    void calculatesIrregularDuration() {
        assertPrice(Duration.ofMinutes(47L).plusSeconds(30L), "95.00");
    }

    @Test
    void exposesElapsedAndBillableDurations() {
        Duration elapsed = Duration.ofMinutes(73L).plusSeconds(11L);

        PricingResult result = pricingService.calculate(
                start,
                start.plus(elapsed),
                hourlyRate);

        assertEquals(elapsed, result.getElapsedDuration());
        assertEquals(elapsed, result.getBillableDuration());
    }

    @Test
    void appliesStandardMonetaryRoundingConsistently() {
        PricingResult result = pricingService.calculate(
                start,
                start.plusSeconds(1L),
                new BigDecimal("100.00"));

        assertEquals(new BigDecimal("0.03"), result.getGamingPrice());
    }

    @Test
    void acceptsReplaceableBillingPolicies() {
        BillableDurationPolicy fixedFifteenMinutes = elapsed -> Duration.ofMinutes(15L);
        PricingService configurableService = new PricingService(
                fixedFifteenMinutes,
                GamingPricePolicy.proratedHourlyRate(),
                MonetaryRoundingPolicy.standardCurrency());

        PricingResult result = configurableService.calculate(
                start,
                start.plusSeconds(1L),
                new BigDecimal("100.00"));

        assertEquals(Duration.ofSeconds(1L), result.getElapsedDuration());
        assertEquals(Duration.ofMinutes(15L), result.getBillableDuration());
        assertEquals(new BigDecimal("25.00"), result.getGamingPrice());
    }

    @Test
    void rejectsCalculationBeforeStart() {
        assertThrows(IllegalArgumentException.class, () -> pricingService.calculate(
                start,
                start.minusSeconds(1L),
                hourlyRate));
    }

    @Test
    void configurableIntervalRoundsOnlyAfterExactBoundaryIncludingNanoseconds() {
        ApplicationSettings settings = new ApplicationSettings(
                "Game Cafe", "EGP", "", null, 15);
        PricingService intervalPricing = new PricingService(
                new SettingsBillableDurationPolicy(() -> settings),
                GamingPricePolicy.proratedHourlyRate(),
                MonetaryRoundingPolicy.standardCurrency());

        PricingResult exactBoundary = intervalPricing.calculate(
                start,
                start.plus(Duration.ofMinutes(15L)),
                hourlyRate);
        PricingResult justBeyondBoundary = intervalPricing.calculate(
                start,
                start.plus(Duration.ofMinutes(15L).plusNanos(1L)),
                hourlyRate);

        assertEquals(Duration.ofMinutes(15L), exactBoundary.getBillableDuration());
        assertEquals(new BigDecimal("30.00"), exactBoundary.getGamingPrice());
        assertEquals(Duration.ofMinutes(30L), justBeyondBoundary.getBillableDuration());
        assertEquals(new BigDecimal("60.00"), justBeyondBoundary.getGamingPrice());
    }

    private void assertPrice(Duration duration, String expectedPrice) {
        PricingResult result = pricingService.calculate(
                start,
                start.plus(duration),
                hourlyRate);

        assertEquals(duration, result.getElapsedDuration());
        assertEquals(duration, result.getBillableDuration());
        assertEquals(new BigDecimal(expectedPrice), result.getGamingPrice());
    }
}
