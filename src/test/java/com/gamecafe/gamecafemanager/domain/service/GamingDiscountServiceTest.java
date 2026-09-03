package com.gamecafe.gamecafemanager.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gamecafe.gamecafemanager.domain.model.GamingDiscount;
import com.gamecafe.gamecafemanager.domain.service.pricing.GamingDiscountResult;
import com.gamecafe.gamecafemanager.domain.service.pricing.MonetaryRoundingPolicy;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class GamingDiscountServiceTest {

    private final GamingDiscountService service = new GamingDiscountService(
            MonetaryRoundingPolicy.standardCurrency());

    @Test
    void supportsEveryAllowedDiscountWithCurrencyRounding() {
        assertDiscount(GamingDiscount.NONE, "200.00", "0.00", "200.00");
        assertDiscount(GamingDiscount.TEN_PERCENT, "200.00", "20.00", "180.00");
        assertDiscount(GamingDiscount.TWENTY_PERCENT, "200.00", "40.00", "160.00");
        assertDiscount(GamingDiscount.FIFTY_PERCENT, "200.00", "100.00", "100.00");

        GamingDiscountResult rounded = service.apply(
                new BigDecimal("37.13"), GamingDiscount.TEN_PERCENT);
        assertEquals(new BigDecimal("3.71"), rounded.getDiscountAmount());
        assertEquals(new BigDecimal("33.42"), rounded.getDiscountedGamingCost());
    }

    @Test
    void rejectsNegativeGamingCostAndUnsupportedPercentages() {
        assertThrows(IllegalArgumentException.class, () ->
                service.apply(new BigDecimal("-0.01"), GamingDiscount.TEN_PERCENT));
        assertThrows(IllegalArgumentException.class, () ->
                GamingDiscount.fromPercentage(15));
    }

    private void assertDiscount(
            GamingDiscount discount,
            String original,
            String amount,
            String discounted) {
        GamingDiscountResult result = service.apply(
                new BigDecimal(original), discount);
        assertEquals(new BigDecimal(original), result.getOriginalGamingCost());
        assertEquals(discount, result.getDiscount());
        assertEquals(new BigDecimal(amount), result.getDiscountAmount());
        assertEquals(new BigDecimal(discounted), result.getDiscountedGamingCost());
    }
}
