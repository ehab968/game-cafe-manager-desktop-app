package com.gamecafe.gamecafemanager.domain.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Read-only checkout quote calculated from persisted session snapshots.
 */
public final class CheckoutSummary {

    private final long sessionId;
    private final String stationName;
    private final StationType stationType;
    private final SessionMode mode;
    private final BigDecimal hourlyRateSnapshot;
    private final Instant startTime;
    private final Instant endTime;
    private final Duration duration;
    private final BigDecimal gamingCost;
    private final GamingDiscount gamingDiscount;
    private final BigDecimal gamingDiscountAmount;
    private final BigDecimal discountedGamingCost;
    private final List<SessionProduct> purchasedProducts;
    private final BigDecimal productsTotal;
    private final BigDecimal finalTotal;

    public CheckoutSummary(
            long sessionId,
            String stationName,
            StationType stationType,
            SessionMode mode,
            BigDecimal hourlyRateSnapshot,
            Instant startTime,
            Instant endTime,
            Duration duration,
            BigDecimal gamingCost,
            List<SessionProduct> purchasedProducts,
            BigDecimal productsTotal,
            BigDecimal finalTotal) {
        this(
                sessionId,
                stationName,
                stationType,
                mode,
                hourlyRateSnapshot,
                startTime,
                endTime,
                duration,
                gamingCost,
                GamingDiscount.NONE,
                BigDecimal.ZERO.setScale(2),
                gamingCost,
                purchasedProducts,
                productsTotal,
                finalTotal);
    }

    public CheckoutSummary(
            long sessionId,
            String stationName,
            StationType stationType,
            SessionMode mode,
            BigDecimal hourlyRateSnapshot,
            Instant startTime,
            Instant endTime,
            Duration duration,
            BigDecimal gamingCost,
            GamingDiscount gamingDiscount,
            BigDecimal gamingDiscountAmount,
            BigDecimal discountedGamingCost,
            List<SessionProduct> purchasedProducts,
            BigDecimal productsTotal,
            BigDecimal finalTotal) {
        this.sessionId = sessionId;
        this.stationName = Objects.requireNonNull(stationName, "stationName");
        this.stationType = Objects.requireNonNull(stationType, "stationType");
        this.mode = mode;
        this.hourlyRateSnapshot = Objects.requireNonNull(
                hourlyRateSnapshot, "hourlyRateSnapshot");
        this.startTime = Objects.requireNonNull(startTime, "startTime");
        this.endTime = Objects.requireNonNull(endTime, "endTime");
        this.duration = Objects.requireNonNull(duration, "duration");
        this.gamingCost = Objects.requireNonNull(gamingCost, "gamingCost");
        this.gamingDiscount = Objects.requireNonNull(gamingDiscount, "gamingDiscount");
        this.gamingDiscountAmount = Objects.requireNonNull(
                gamingDiscountAmount, "gamingDiscountAmount");
        this.discountedGamingCost = Objects.requireNonNull(
                discountedGamingCost, "discountedGamingCost");
        this.purchasedProducts = Collections.unmodifiableList(
                new ArrayList<>(Objects.requireNonNull(
                        purchasedProducts, "purchasedProducts")));
        this.productsTotal = Objects.requireNonNull(productsTotal, "productsTotal");
        this.finalTotal = Objects.requireNonNull(finalTotal, "finalTotal");
    }

    public long getSessionId() {
        return sessionId;
    }

    public String getStationName() {
        return stationName;
    }

    public StationType getStationType() {
        return stationType;
    }

    public SessionMode getMode() {
        return mode;
    }

    public BigDecimal getHourlyRateSnapshot() {
        return hourlyRateSnapshot;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public Duration getDuration() {
        return duration;
    }

    public BigDecimal getGamingCost() {
        return gamingCost;
    }

    public GamingDiscount getGamingDiscount() {
        return gamingDiscount;
    }

    public BigDecimal getGamingDiscountAmount() {
        return gamingDiscountAmount;
    }

    public BigDecimal getDiscountedGamingCost() {
        return discountedGamingCost;
    }

    public List<SessionProduct> getPurchasedProducts() {
        return purchasedProducts;
    }

    public BigDecimal getProductsTotal() {
        return productsTotal;
    }

    public BigDecimal getFinalTotal() {
        return finalTotal;
    }
}
