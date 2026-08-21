package com.gamecafe.gamecafemanager.data.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

final class V3SessionTotalsMigration implements SQLiteMigration {

    @Override
    public int version() {
        return 3;
    }

    @Override
    public String description() {
        return "Add product and final totals to sessions";
    }

    @Override
    public void apply(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "ALTER TABLE sessions ADD COLUMN products_total_minor "
                            + "INTEGER NOT NULL DEFAULT 0 CHECK (products_total_minor >= 0)");
            statement.execute(
                    "ALTER TABLE sessions ADD COLUMN final_total_minor "
                            + "INTEGER NOT NULL DEFAULT 0 CHECK (final_total_minor >= 0)");
        }
    }
}
