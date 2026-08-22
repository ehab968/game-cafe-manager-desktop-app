package com.gamecafe.gamecafemanager.data.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

final class V7UsersMigration implements SQLiteMigration {

    @Override
    public int version() {
        return 7;
    }

    @Override
    public String description() {
        return "Add local users and password verifiers";
    }

    @Override
    public void apply(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE users ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + "username TEXT NOT NULL COLLATE NOCASE UNIQUE, "
                            + "role TEXT NOT NULL CHECK (role IN ('ADMIN', 'CASHIER')), "
                            + "password_algorithm TEXT NOT NULL, "
                            + "password_salt BLOB NOT NULL, "
                            + "password_iterations INTEGER NOT NULL "
                            + "CHECK (password_iterations > 0), "
                            + "password_hash BLOB NOT NULL, "
                            + "enabled INTEGER NOT NULL DEFAULT 1 CHECK (enabled IN (0, 1)), "
                            + "created_at TEXT NOT NULL DEFAULT "
                            + "(strftime('%Y-%m-%dT%H:%M:%fZ', 'now')), "
                            + "updated_at TEXT NOT NULL DEFAULT "
                            + "(strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))"
                            + ")");
        }
    }
}
