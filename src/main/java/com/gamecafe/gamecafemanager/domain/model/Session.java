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
    private final Instant startTime;
    private final Instant endTime;
    private final SessionStatus status;
    private final BigDecimal hourlyRateSnapshot;
    private final BigDecimal playCost;
    private final BigDecimal productsCost;
    private final BigDecimal finalTotal;

    public Session(
            Long id,
            long stationId,
            String stationNameSnapshot,
            Instant startTime,
            Instant endTime,
            SessionStatus status,
            BigDecimal hourlyRateSnapshot,
            BigDecimal playCost,
            BigDecimal productsCost,
            BigDecimal finalTotal) {
        this.id = id;
        this.stationId = stationId;
        this.stationNameSnapshot = Objects.requireNonNull(
                stationNameSnapshot, "stationNameSnapshot");
        this.startTime = Objects.requireNonNull(startTime, "startTime");
        this.endTime = endTime;
        this.status = Objects.requireNonNull(status, "status");
        this.hourlyRateSnapshot = Objects.requireNonNull(
                hourlyRateSnapshot, "hourlyRateSnapshot");
        this.playCost = Objects.requireNonNull(playCost, "playCost");
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

    public BigDecimal getProductsCost() {
        return productsCost;
    }

    public BigDecimal getFinalTotal() {
        return finalTotal;
    }
}
