package com.gamecafe.gamecafemanager.data.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.gamecafe.gamecafemanager.data.repository.SQLiteSessionRepository;
import com.gamecafe.gamecafemanager.data.repository.SQLiteStationRepository;
import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.model.Station;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class V11StationSessionModePricingMigrationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void migratesLegacyRatesWithoutLosingStationOrSessionData() throws SQLException {
        SQLiteDatabase database = new SQLiteDatabase(
                temporaryDirectory.resolve("legacy-v10.db"));
        createRepresentativeVersionTenDatabase(database);

        database.initialize();

        SQLiteStationRepository stations = new SQLiteStationRepository(database);
        Station playStation = stations.findById(1L).orElseThrow(AssertionError::new);
        Station billiard = stations.findById(2L).orElseThrow(AssertionError::new);
        assertEquals("Legacy Room", playStation.getName());
        assertEquals(new BigDecimal("60.00"), playStation.getSingleHourlyRate());
        assertEquals(new BigDecimal("60.00"), playStation.getMultiHourlyRate());
        assertEquals(new BigDecimal("80.00"), billiard.getHourlyRate());
        assertNull(billiard.getMultiHourlyRate());

        Session legacySession = new SQLiteSessionRepository(database)
                .findById(10L)
                .orElseThrow(AssertionError::new);
        assertEquals("Legacy Room", legacySession.getStationNameSnapshot());
        assertNull(legacySession.getMode());
        assertEquals(new BigDecimal("60.00"), legacySession.getHourlyRateSnapshot());
        assertEquals("2026-08-20T18:00:00Z", legacySession.getStartTime().toString());
        assertEquals(14, queryForInt(
                database, "SELECT MAX(version) FROM schema_migrations"));
    }

    private void createRepresentativeVersionTenDatabase(SQLiteDatabase database)
            throws SQLException {
        try (Connection connection = database.openConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE schema_migrations ("
                            + "version INTEGER PRIMARY KEY, description TEXT NOT NULL, "
                            + "applied_at TEXT NOT NULL DEFAULT "
                            + "(strftime('%Y-%m-%dT%H:%M:%fZ', 'now')))" );
            statement.execute(
                    "INSERT INTO schema_migrations(version, description) "
                            + "VALUES (10, 'Legacy version 10')");
            statement.execute(
                    "CREATE TABLE stations ("
                            + "id INTEGER PRIMARY KEY, name TEXT NOT NULL, type TEXT NOT NULL, "
                            + "hourly_rate_minor INTEGER NOT NULL, active INTEGER NOT NULL)" );
            statement.execute(
                    "INSERT INTO stations(id, name, type, hourly_rate_minor, active) VALUES "
                            + "(1, 'Legacy Room', 'PLAYSTATION', 6000, 1), "
                            + "(2, 'Legacy Billiard', 'BILLIARD', 8000, 1)" );
            statement.execute(
                    "CREATE TABLE sessions ("
                            + "id INTEGER PRIMARY KEY, station_id INTEGER NOT NULL, "
                            + "station_name TEXT NOT NULL, station_type TEXT NOT NULL, "
                            + "station_rate_minor INTEGER NOT NULL, start_time TEXT NOT NULL, "
                            + "end_time TEXT, status TEXT NOT NULL, "
                            + "station_total_minor INTEGER NOT NULL, "
                            + "products_total_minor INTEGER NOT NULL, "
                            + "final_total_minor INTEGER NOT NULL)" );
            statement.execute(
                    "INSERT INTO sessions(id, station_id, station_name, station_type, "
                            + "station_rate_minor, start_time, end_time, status, "
                            + "station_total_minor, products_total_minor, final_total_minor) "
                            + "VALUES (10, 1, 'Legacy Room', 'PLAYSTATION', 6000, "
                            + "'2026-08-20T18:00:00Z', NULL, 'ACTIVE', 0, 0, 0)" );
            statement.execute(
                    "CREATE TABLE application_settings ("
                            + "id INTEGER PRIMARY KEY CHECK (id = 1), "
                            + "cafe_name TEXT NOT NULL, currency_display TEXT NOT NULL, "
                            + "invoice_footer TEXT NOT NULL, minimum_session_minutes INTEGER, "
                            + "billing_rounding_minutes INTEGER, updated_at TEXT NOT NULL)" );
            statement.execute(
                    "INSERT INTO application_settings(id, cafe_name, currency_display, "
                            + "invoice_footer, minimum_session_minutes, "
                            + "billing_rounding_minutes, updated_at) VALUES "
                            + "(1, 'Legacy Cafe', 'EGP', 'Thanks', NULL, NULL, "
                            + "'2026-08-20T18:00:00Z')" );
        }
    }

    private int queryForInt(SQLiteDatabase database, String sql) throws SQLException {
        try (Connection connection = database.openConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }
}
