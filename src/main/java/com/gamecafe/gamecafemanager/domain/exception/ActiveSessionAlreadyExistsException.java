package com.gamecafe.gamecafemanager.domain.exception;

public class ActiveSessionAlreadyExistsException extends RuntimeException {

    public ActiveSessionAlreadyExistsException(long stationId) {
        super("Station " + stationId + " already has an active session");
    }
}
