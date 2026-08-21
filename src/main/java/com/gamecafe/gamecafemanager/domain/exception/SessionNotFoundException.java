package com.gamecafe.gamecafemanager.domain.exception;

public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(long sessionId) {
        super("Session " + sessionId + " was not found");
    }
}
