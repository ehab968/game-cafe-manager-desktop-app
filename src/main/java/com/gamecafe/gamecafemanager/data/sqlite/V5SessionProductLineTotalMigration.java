package com.gamecafe.gamecafemanager.data.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

final class V5SessionProductLineTotalMigration implements SQLiteMigration {

    @Override
    public int version() {
        return 5;
    }

    @Override
    public String description() {
        return "Snapshot session product line totals";
    }

    @Override
    public void apply(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "ALTER TABLE session_products ADD COLUMN line_total_minor "
                            + "INTEGER NOT NULL DEFAULT 0 CHECK (line_total_minor >= 0)");
            statement.execute(
                    "UPDATE session_products SET line_total_minor = unit_price_minor * quantity");
        }
    }
}
