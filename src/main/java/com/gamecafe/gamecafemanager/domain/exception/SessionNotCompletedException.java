package com.gamecafe.gamecafemanager.domain.exception;

public final class SessionNotCompletedException extends RuntimeException {

    public SessionNotCompletedException(long sessionId) {
        super("Session " + sessionId + " is not completed");
    }
}
