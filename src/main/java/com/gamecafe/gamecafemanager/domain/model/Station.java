package com.gamecafe.gamecafemanager.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A rentable resource in the game cafe.
 */
public final class Station {

    private final Long id;
    private final String name;
    private final StationType type;
    private final BigDecimal singleHourlyRate;
    private final BigDecimal multiHourlyRate;
    private final boolean enabled;

    /**
     * Creates a station with one hourly rate. This is the natural constructor
     * for station types such as Billiard that do not support session modes.
     */
    public Station(
            Long id,
            String name,
            StationType type,
            BigDecimal hourlyRate,
            boolean enabled) {
        this(id, name, type, hourlyRate, null, enabled);
    }

    /**
     * Creates a Single/Multi-priced station. The first rate remains the
     * primary rate so existing one-rate persistence can be migrated safely.
     */
    public Station(
            Long id,
            String name,
            StationType type,
            BigDecimal singleHourlyRate,
            BigDecimal multiHourlyRate,
            boolean enabled) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.singleHourlyRate = Objects.requireNonNull(
                singleHourlyRate, "singleHourlyRate");
        if (type.supportsSessionModes()) {
            this.multiHourlyRate = Objects.requireNonNull(
                    multiHourlyRate, "multiHourlyRate");
        } else {
            if (multiHourlyRate != null) {
                throw new IllegalArgumentException(
                        type.getDisplayName() + " does not support Multi pricing");
            }
            this.multiHourlyRate = null;
        }
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public StationType getType() {
        return type;
    }

    public BigDecimal getHourlyRate() {
        return singleHourlyRate;
    }

    public BigDecimal getSingleHourlyRate() {
        return singleHourlyRate;
    }

    public BigDecimal getMultiHourlyRate() {
        return multiHourlyRate;
    }

    public BigDecimal getRateForMode(SessionMode mode) {
        if (!type.supportsSessionModes()) {
            if (mode != null) {
                throw new IllegalArgumentException(
                        type.getDisplayName() + " does not support a session mode");
            }
            return singleHourlyRate;
        }
        if (mode == null) {
            throw new IllegalArgumentException("Session mode is required for " + type);
        }
        return mode == SessionMode.SINGLE ? singleHourlyRate : multiHourlyRate;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
