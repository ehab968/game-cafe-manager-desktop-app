package com.gamecafe.gamecafemanager.data.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Adds mode pricing without rebuilding tables or discarding legacy prices.
 */
final class V11StationSessionModePricingMigration implements SQLiteMigration {

    @Override
    public int version() {
        return 11;
    }

    @Override
    public String description() {
        return "Add Single and Multi station pricing and session mode snapshots";
    }

    @Override
    public void apply(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "ALTER TABLE stations ADD COLUMN single_hourly_rate_minor "
                            + "INTEGER CHECK (single_hourly_rate_minor >= 0)");
            statement.execute(
                    "ALTER TABLE stations ADD COLUMN multi_hourly_rate_minor "
                            + "INTEGER CHECK (multi_hourly_rate_minor >= 0)");
            statement.execute(
                    "UPDATE stations SET "
                            + "single_hourly_rate_minor = hourly_rate_minor, "
                            + "multi_hourly_rate_minor = hourly_rate_minor "
                            + "WHERE type IN ('PLAYSTATION', 'PING_PONG')");
            statement.execute(
                    "ALTER TABLE sessions ADD COLUMN session_mode TEXT "
                            + "CHECK (session_mode IN ('SINGLE', 'MULTI'))");
        }
    }
}
