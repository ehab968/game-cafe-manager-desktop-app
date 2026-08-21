package com.gamecafe.gamecafemanager.domain.exception;

/**
 * Raised when an operation targets a station that no longer exists.
 */
public class StationNotFoundException extends RuntimeException {

    public StationNotFoundException(long stationId) {
        super("Station " + stationId + " was not found");
    }
}
