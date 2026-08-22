package com.gamecafe.gamecafemanager.data.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Stores the single local user whose sign-in should be restored at startup.
 */
final class V10RememberedAuthenticationMigration implements SQLiteMigration {

    @Override
    public int version() {
        return 10;
    }

    @Override
    public String description() {
        return "Remember the authenticated local user";
    }

    @Override
    public void apply(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE remembered_authentication ("
                            + "id INTEGER PRIMARY KEY CHECK (id = 1), "
                            + "user_id INTEGER NOT NULL UNIQUE, "
                            + "remembered_at TEXT NOT NULL DEFAULT "
                            + "(strftime('%Y-%m-%dT%H:%M:%fZ', 'now')), "
                            + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE"
                            + ")");
        }
    }
}
