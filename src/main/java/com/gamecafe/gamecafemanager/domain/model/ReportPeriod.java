package com.gamecafe.gamecafemanager.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Inclusive local-date period and its corresponding persisted-time bounds.
 */
public final class ReportPeriod {

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Instant startInclusive;
    private final Instant endExclusive;

    public ReportPeriod(LocalDate startDate, LocalDate endDate, ZoneId zoneId) {
        this.startDate = Objects.requireNonNull(startDate, "startDate");
        this.endDate = Objects.requireNonNull(endDate, "endDate");
        Objects.requireNonNull(zoneId, "zoneId");
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Report end date cannot be before start date");
        }
        startInclusive = startDate.atStartOfDay(zoneId).toInstant();
        endExclusive = endDate.plusDays(1L).atStartOfDay(zoneId).toInstant();
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Instant getStartInclusive() {
        return startInclusive;
    }

    public Instant getEndExclusive() {
        return endExclusive;
    }
}
