package com.gamecafe.gamecafemanager.domain.exception;

/**
 * Raised when a station is disabled while it still has an active session.
 */
public final class StationInUseException extends RuntimeException {

    private final long stationId;

    public StationInUseException(long stationId) {
        super("Station " + stationId + " cannot be disabled while a session is active");
        this.stationId = stationId;
    }

    public long getStationId() {
        return stationId;
    }
}
