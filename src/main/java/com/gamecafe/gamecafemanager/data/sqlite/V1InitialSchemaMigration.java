package com.gamecafe.gamecafemanager.data.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

final class V1InitialSchemaMigration implements SQLiteMigration {

    private static final String[] STATEMENTS = {
        "CREATE TABLE stations ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL UNIQUE, "
                + "type TEXT NOT NULL CHECK (type IN ('PLAYSTATION', 'BILLIARD', 'PING_PONG')), "
                + "hourly_rate_minor INTEGER NOT NULL CHECK (hourly_rate_minor >= 0), "
                + "active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)), "
                + "created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')), "
                + "updated_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))"
                + ")",
        "CREATE TABLE sessions ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "station_id INTEGER NOT NULL, "
                + "station_name TEXT NOT NULL, "
                + "station_rate_minor INTEGER NOT NULL CHECK (station_rate_minor >= 0), "
                + "start_time TEXT NOT NULL, "
                + "end_time TEXT, "
                + "status TEXT NOT NULL CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED')), "
                + "station_total_minor INTEGER CHECK (station_total_minor >= 0), "
                + "created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')), "
                + "updated_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')), "
                + "FOREIGN KEY (station_id) REFERENCES stations(id) ON UPDATE CASCADE ON DELETE RESTRICT, "
                + "CHECK (end_time IS NULL OR end_time >= start_time)"
                + ")",
        "CREATE UNIQUE INDEX one_active_session_per_station "
                + "ON sessions(station_id) WHERE status = 'ACTIVE'",
        "CREATE INDEX sessions_station_id_index ON sessions(station_id)",
        "CREATE INDEX sessions_status_index ON sessions(status)",
        "CREATE TABLE products ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL UNIQUE, "
                + "current_price_minor INTEGER NOT NULL CHECK (current_price_minor >= 0), "
                + "stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0), "
                + "active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)), "
                + "created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')), "
                + "updated_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))"
                + ")",
        "CREATE TABLE session_products ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "session_id INTEGER NOT NULL, "
                + "product_id INTEGER NOT NULL, "
                + "product_name TEXT NOT NULL, "
                + "quantity INTEGER NOT NULL CHECK (quantity > 0), "
                + "unit_price_minor INTEGER NOT NULL CHECK (unit_price_minor >= 0), "
                + "created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')), "
                + "FOREIGN KEY (session_id) REFERENCES sessions(id) ON UPDATE CASCADE ON DELETE RESTRICT, "
                + "FOREIGN KEY (product_id) REFERENCES products(id) ON UPDATE CASCADE ON DELETE RESTRICT"
                + ")",
        "CREATE INDEX session_products_session_id_index ON session_products(session_id)",
        "CREATE INDEX session_products_product_id_index ON session_products(product_id)"
    };

    @Override
    public int version() {
        return 1;
    }

    @Override
    public String description() {
        return "Create stations, sessions, products, and session products";
    }

    @Override
    public void apply(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String sql : STATEMENTS) {
                statement.execute(sql);
            }
        }
    }
}
