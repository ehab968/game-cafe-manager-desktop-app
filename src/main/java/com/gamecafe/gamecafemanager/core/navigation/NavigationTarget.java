package com.gamecafe.gamecafemanager.core.navigation;

/**
 * Destinations available in the application shell.
 *
 * Business-feature destinations will be introduced only in their respective
 * implementation phases.
 */
public enum NavigationTarget {
    DASHBOARD(
            "Dashboard",
            "Monitor all stations and active sessions."),
    WELCOME(
            "Welcome",
            "The cafe management application is ready. Use the navigation menu to move between application areas."),
    STATIONS(
            "Stations",
            "Manage the rentable resources available in the game cafe."),
    PRODUCTS(
            "Products",
            "Manage inventory products, current prices, and stock."),
    USERS(
            "Users",
            "Manage local users and roles."),
    REPORTS(
            "Reports",
            "Review completed-session revenue and usage."),
    SETTINGS(
            "Settings",
            "Configure cafe branding, invoices, and session billing."),
    ACTIVE_SESSIONS(
            "Active Sessions",
            "Monitor persisted sessions and their current elapsed time."),
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
