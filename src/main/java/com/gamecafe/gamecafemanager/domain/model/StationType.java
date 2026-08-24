package com.gamecafe.gamecafemanager.domain.model;

/**
 * Types of rentable resources supported by the station catalog.
 */
public enum StationType {
    PLAYSTATION("PlayStation room", true),
    BILLIARD("Billiard table", false),
    PING_PONG("Ping-pong table", true);

    private final String displayName;
    private final boolean sessionModesSupported;

    StationType(String displayName, boolean sessionModesSupported) {
        this.displayName = displayName;
        this.sessionModesSupported = sessionModesSupported;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean supportsSessionModes() {
        return sessionModesSupported;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
