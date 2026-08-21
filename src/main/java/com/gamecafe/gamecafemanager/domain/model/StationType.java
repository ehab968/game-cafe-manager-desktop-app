package com.gamecafe.gamecafemanager.domain.model;

/**
 * Types of rentable resources supported by the station catalog.
 */
public enum StationType {
    PLAYSTATION("PlayStation room"),
    BILLIARD("Billiard table"),
    PING_PONG("Ping-pong table");

    private final String displayName;

    StationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
