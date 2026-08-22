package com.gamecafe.gamecafemanager.data.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

final class V9ApplicationSettingsMigration implements SQLiteMigration {

    @Override
    public int version() {
        return 9;
    }

    @Override
    public String description() {
        return "Add application settings";
    }

    @Override
    public void apply(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE application_settings ("
                            + "id INTEGER PRIMARY KEY CHECK (id = 1), "
                            + "cafe_name TEXT NOT NULL, "
                            + "currency_display TEXT NOT NULL, "
                            + "invoice_footer TEXT NOT NULL, "
                            + "minimum_session_minutes INTEGER "
                            + "CHECK (minimum_session_minutes IS NULL "
                            + "OR minimum_session_minutes BETWEEN 1 AND 1440), "
                            + "billing_rounding_minutes INTEGER "
                            + "CHECK (billing_rounding_minutes IS NULL "
                            + "OR billing_rounding_minutes BETWEEN 1 AND 1440), "
                            + "updated_at TEXT NOT NULL DEFAULT "
                            + "(strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))"
                            + ")");
        }
    }
}
