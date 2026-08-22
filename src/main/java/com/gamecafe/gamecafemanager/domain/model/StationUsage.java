package com.gamecafe.gamecafemanager.domain.model;

import java.time.Duration;
import java.util.Objects;

public final class StationUsage {

    private final long stationId;
    private final String stationName;
    private final long completedSessions;
    private final Duration totalDuration;

    public StationUsage(
            long stationId,
            String stationName,
            long completedSessions,
            Duration totalDuration) {
        this.stationId = stationId;
        this.stationName = Objects.requireNonNull(stationName, "stationName");
        this.completedSessions = completedSessions;
        this.totalDuration = Objects.requireNonNull(totalDuration, "totalDuration");
    }

    public long getStationId() {
        return stationId;
    }

    public String getStationName() {
        return stationName;
    }

    public long getCompletedSessions() {
        return completedSessions;
    }

    public Duration getTotalDuration() {
        return totalDuration;
    }
}
