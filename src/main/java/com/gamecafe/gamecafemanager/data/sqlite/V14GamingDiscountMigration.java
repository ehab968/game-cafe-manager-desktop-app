package com.gamecafe.gamecafemanager.data.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Adds immutable gaming-discount checkout snapshots while preserving every
 * legacy total as a natural no-discount transaction.
 */
final class V14GamingDiscountMigration implements SQLiteMigration {

    @Override
    public int version() {
        return 14;
    }

    @Override
    public String description() {
        return "Add gaming discount snapshots";
    }

    @Override
    public void apply(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "ALTER TABLE sessions ADD COLUMN gaming_discount_percent "
                            + "INTEGER NOT NULL DEFAULT 0 "
                            + "CHECK (gaming_discount_percent IN (0, 10, 20, 50))");
            statement.execute(
                    "ALTER TABLE sessions ADD COLUMN gaming_discount_minor "
                            + "INTEGER NOT NULL DEFAULT 0 "
                            + "CHECK (gaming_discount_minor >= 0)");
            statement.execute(
                    "ALTER TABLE sessions ADD COLUMN discounted_gaming_total_minor "
                            + "INTEGER NOT NULL DEFAULT 0 "
                            + "CHECK (discounted_gaming_total_minor >= 0)");
            statement.executeUpdate(
                    "UPDATE sessions SET discounted_gaming_total_minor "
                            + "= station_total_minor");
        }
    }
}
