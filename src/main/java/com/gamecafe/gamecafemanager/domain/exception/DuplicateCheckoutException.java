package com.gamecafe.gamecafemanager.domain.exception;

/**
 * Raised when checkout is attempted for a session that is already completed.
 */
public final class DuplicateCheckoutException extends RuntimeException {

    private final long sessionId;

    public DuplicateCheckoutException(long sessionId) {
        super("Session " + sessionId + " has already been checked out");
        this.sessionId = sessionId;
    }

    public long getSessionId() {
        return sessionId;
    }
}
