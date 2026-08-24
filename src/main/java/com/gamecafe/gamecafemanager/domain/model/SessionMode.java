package com.gamecafe.gamecafemanager.domain.model;

/**
 * Occupancy/pricing mode selected when a supported station session starts.
 */
public enum SessionMode {
    SINGLE("Single"),
    MULTI("Multi");

    private final String displayName;

    SessionMode(String displayName) {
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
