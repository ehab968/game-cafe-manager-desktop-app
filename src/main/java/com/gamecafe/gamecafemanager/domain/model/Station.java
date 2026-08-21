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
    private final BigDecimal hourlyRate;
    private final boolean enabled;

    public Station(
            Long id,
            String name,
            StationType type,
            BigDecimal hourlyRate,
            boolean enabled) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.hourlyRate = Objects.requireNonNull(hourlyRate, "hourlyRate");
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
        return hourlyRate;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
