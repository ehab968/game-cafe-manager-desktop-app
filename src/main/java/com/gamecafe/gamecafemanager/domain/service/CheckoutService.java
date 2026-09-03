package com.gamecafe.gamecafemanager.domain.service;

import com.gamecafe.gamecafemanager.domain.exception.SessionNotActiveException;
import com.gamecafe.gamecafemanager.domain.model.CheckoutSummary;
import com.gamecafe.gamecafemanager.domain.model.GamingDiscount;
import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.model.SessionProduct;
import com.gamecafe.gamecafemanager.domain.model.SessionStatus;
import com.gamecafe.gamecafemanager.domain.service.pricing.PricingResult;
import com.gamecafe.gamecafemanager.domain.service.pricing.GamingDiscountResult;
import com.gamecafe.gamecafemanager.domain.service.pricing.MonetaryRoundingPolicy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Calculates checkout totals from persisted station and product snapshots.
 */
public final class CheckoutService {

    private static final BigDecimal ZERO_MONEY =
            BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

    private final PricingService pricingService;
    private final GamingDiscountService gamingDiscountService;

    public CheckoutService(PricingService pricingService) {
        this(
                pricingService,
                new GamingDiscountService(MonetaryRoundingPolicy.standardCurrency()));
    }

    public CheckoutService(
            PricingService pricingService,
            GamingDiscountService gamingDiscountService) {
        this.pricingService = Objects.requireNonNull(pricingService, "pricingService");
        this.gamingDiscountService = Objects.requireNonNull(
                gamingDiscountService, "gamingDiscountService");
    }

    public CheckoutSummary calculate(
            Session session,
            List<SessionProduct> purchasedProducts,
            Instant endTime) {
        return calculate(session, purchasedProducts, endTime, GamingDiscount.NONE);
    }

    public CheckoutSummary calculate(
            Session session,
            List<SessionProduct> purchasedProducts,
            Instant endTime,
            GamingDiscount gamingDiscount) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(purchasedProducts, "purchasedProducts");
        Objects.requireNonNull(endTime, "endTime");
        Objects.requireNonNull(gamingDiscount, "gamingDiscount");
        if (session.getId() == null) {
            throw new IllegalArgumentException("Persisted session id is required for checkout");
        }
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new SessionNotActiveException(session.getId());
        }

        PricingResult pricing = pricingService.calculate(
                session.getStartTime(), endTime, session.getHourlyRateSnapshot());
        BigDecimal productsTotal = ZERO_MONEY;
        for (SessionProduct item : purchasedProducts) {
            if (item.getSessionId() != session.getId()) {
                throw new IllegalArgumentException("Checkout item belongs to another session");
            }
            productsTotal = productsTotal.add(item.getLineTotal());
        }
        productsTotal = productsTotal.setScale(2, RoundingMode.UNNECESSARY);
        GamingDiscountResult discountResult = gamingDiscountService.apply(
                pricing.getGamingPrice(), gamingDiscount);
        BigDecimal finalTotal = discountResult.getDiscountedGamingCost()
                .add(productsTotal)
                .setScale(2, RoundingMode.UNNECESSARY);

        return new CheckoutSummary(
                session.getId(),
                session.getStationNameSnapshot(),
                session.getStationTypeSnapshot(),
                session.getMode(),
                session.getHourlyRateSnapshot(),
                session.getStartTime(),
                endTime,
                pricing.getElapsedDuration(),
                discountResult.getOriginalGamingCost(),
                discountResult.getDiscount(),
                discountResult.getDiscountAmount(),
                discountResult.getDiscountedGamingCost(),
                purchasedProducts,
                productsTotal,
                finalTotal);
    }
}
