package com.gamecafe.gamecafemanager.domain.exception;

public class SessionNotActiveException extends RuntimeException {

    public SessionNotActiveException(long sessionId) {
        super("Session " + sessionId + " is not active");
    }
}
