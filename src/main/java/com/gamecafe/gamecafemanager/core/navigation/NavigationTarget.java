package com.gamecafe.gamecafemanager.core.navigation;

/**
 * Destinations available in the application shell.
 *
 * Business-feature destinations will be introduced only in their respective
 * implementation phases.
 */
public enum NavigationTarget {
    WELCOME(
            "Welcome",
            "Game Cafe Manager is ready. Use the navigation menu to move between application areas."),
    ABOUT(
            "About",
            "This is the initial desktop application foundation. Business features are not enabled yet.");

    private final String title;
    private final String description;

    NavigationTarget(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
