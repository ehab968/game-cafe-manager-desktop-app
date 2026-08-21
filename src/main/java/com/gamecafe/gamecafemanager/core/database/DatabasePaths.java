package com.gamecafe.gamecafemanager.core.database;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves the persistent database location without a platform-specific library.
 */
public final class DatabasePaths {

    public static final String DATABASE_PATH_PROPERTY = "gamecafe.database.path";
    public static final String DATABASE_PATH_ENVIRONMENT_VARIABLE = "GAME_CAFE_DATABASE_PATH";

    private static final String APPLICATION_DIRECTORY = ".game-cafe-manager";
    private static final String DATABASE_FILE = "game-cafe.db";

    private DatabasePaths() {
    }

    public static Path defaultDatabasePath() {
        String configuredPath = System.getProperty(DATABASE_PATH_PROPERTY);
        if (configuredPath == null || configuredPath.trim().isEmpty()) {
            configuredPath = System.getenv(DATABASE_PATH_ENVIRONMENT_VARIABLE);
        }

        if (configuredPath != null && !configuredPath.trim().isEmpty()) {
            return Paths.get(configuredPath.trim()).toAbsolutePath().normalize();
        }

        return Paths.get(System.getProperty("user.home"), APPLICATION_DIRECTORY, DATABASE_FILE)
                .toAbsolutePath()
                .normalize();
    }
}
