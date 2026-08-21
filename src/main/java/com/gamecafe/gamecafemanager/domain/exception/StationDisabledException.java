package com.gamecafe.gamecafemanager.domain.exception;

public class StationDisabledException extends RuntimeException {

    public StationDisabledException(long stationId) {
        super("Station " + stationId + " is disabled");
    }
}
