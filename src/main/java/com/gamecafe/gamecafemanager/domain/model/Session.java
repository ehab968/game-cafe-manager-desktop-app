package com.gamecafe.gamecafemanager.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * A persisted rental of one station. Rate and cost values are snapshots so
 * later station-price changes cannot alter session history.
 */
public final class Session {

    private final Long id;
    private final long stationId;
    private final String stationNameSnapshot;
    private final StationType stationTypeSnapshot;
    private final SessionMode mode;
    private final Instant startTime;
    private final Instant endTime;
    private final SessionStatus status;
    private final BigDecimal hourlyRateSnapshot;
    private final BigDecimal playCost;
    private final GamingDiscount gamingDiscount;
    private final BigDecimal gamingDiscountAmount;
    private final BigDecimal discountedPlayCost;
    private final BigDecimal productsCost;
    private final BigDecimal finalTotal;

    public Session(
            Long id,
            long stationId,
            String stationNameSnapshot,
            StationType stationTypeSnapshot,
            SessionMode mode,
            Instant startTime,
            Instant endTime,
            SessionStatus status,
            BigDecimal hourlyRateSnapshot,
            BigDecimal playCost,
            BigDecimal productsCost,
            BigDecimal finalTotal) {
        this(
                id,
                stationId,
                stationNameSnapshot,
                stationTypeSnapshot,
                mode,
                startTime,
                endTime,
                status,
                hourlyRateSnapshot,
                playCost,
                GamingDiscount.NONE,
                BigDecimal.ZERO.setScale(2),
                playCost,
                productsCost,
                finalTotal);
    }

    public Session(
            Long id,
            long stationId,
            String stationNameSnapshot,
            StationType stationTypeSnapshot,
            SessionMode mode,
            Instant startTime,
            Instant endTime,
            SessionStatus status,
            BigDecimal hourlyRateSnapshot,
            BigDecimal playCost,
            GamingDiscount gamingDiscount,
            BigDecimal gamingDiscountAmount,
            BigDecimal discountedPlayCost,
            BigDecimal productsCost,
            BigDecimal finalTotal) {
        this.id = id;
        this.stationId = stationId;
        this.stationNameSnapshot = Objects.requireNonNull(
                stationNameSnapshot, "stationNameSnapshot");
        this.stationTypeSnapshot = Objects.requireNonNull(
                stationTypeSnapshot, "stationTypeSnapshot");
        this.mode = mode;
        this.startTime = Objects.requireNonNull(startTime, "startTime");
        this.endTime = endTime;
        this.status = Objects.requireNonNull(status, "status");
        this.hourlyRateSnapshot = Objects.requireNonNull(
                hourlyRateSnapshot, "hourlyRateSnapshot");
        this.playCost = Objects.requireNonNull(playCost, "playCost");
        this.gamingDiscount = Objects.requireNonNull(gamingDiscount, "gamingDiscount");
        this.gamingDiscountAmount = Objects.requireNonNull(
                gamingDiscountAmount, "gamingDiscountAmount");
        this.discountedPlayCost = Objects.requireNonNull(
                discountedPlayCost, "discountedPlayCost");
        this.productsCost = Objects.requireNonNull(productsCost, "productsCost");
        this.finalTotal = Objects.requireNonNull(finalTotal, "finalTotal");
    }

    public Long getId() {
        return id;
    }

    public long getStationId() {
        return stationId;
    }

    public String getStationNameSnapshot() {
        return stationNameSnapshot;
    }

    public StationType getStationTypeSnapshot() {
        return stationTypeSnapshot;
    }

    /**
     * Returns the selected mode, or null for Billiard and migrated legacy
     * sessions whose original mode cannot be inferred.
     */
    public SessionMode getMode() {
        return mode;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public BigDecimal getHourlyRateSnapshot() {
        return hourlyRateSnapshot;
    }

    public BigDecimal getPlayCost() {
        return playCost;
    }

    public GamingDiscount getGamingDiscount() {
        return gamingDiscount;
    }

    public BigDecimal getGamingDiscountAmount() {
        return gamingDiscountAmount;
    }

    public BigDecimal getDiscountedPlayCost() {
        return discountedPlayCost;
    }

    public BigDecimal getProductsCost() {
        return productsCost;
    }

    public BigDecimal getFinalTotal() {
        return finalTotal;
    }
}
