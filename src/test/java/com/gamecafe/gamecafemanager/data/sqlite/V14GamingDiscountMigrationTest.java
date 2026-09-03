package com.gamecafe.gamecafemanager.data.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.gamecafe.gamecafemanager.data.repository.SQLiteSessionRepository;
import com.gamecafe.gamecafemanager.domain.model.ApplicationSettings;
import com.gamecafe.gamecafemanager.domain.model.GamingDiscount;
import com.gamecafe.gamecafemanager.domain.model.Invoice;
import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.service.InvoiceService;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class V14GamingDiscountMigrationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void preservesLegacyCompletedSessionAndTreatsItAsNoDiscount()
            throws SQLException {
        SQLiteDatabase database = new SQLiteDatabase(
                temporaryDirectory.resolve("legacy-v13.db"));
        createRepresentativeVersionThirteenDatabase(database);

        database.initialize();

        assertEquals(14, queryForLong(
                database, "SELECT MAX(version) FROM schema_migrations"));
        assertEquals(0, queryForLong(
                database, "SELECT gaming_discount_percent FROM sessions WHERE id = 9"));
        assertEquals(0, queryForLong(
                database, "SELECT gaming_discount_minor FROM sessions WHERE id = 9"));
        assertEquals(8_000L, queryForLong(
                database,
                "SELECT discounted_gaming_total_minor FROM sessions WHERE id = 9"));
        assertEquals(10_000L, queryForLong(
                database, "SELECT final_total_minor FROM sessions WHERE id = 9"));

        Session legacy = new SQLiteSessionRepository(database)
                .findById(9L)
                .orElseThrow(AssertionError::new);
        assertEquals(GamingDiscount.NONE, legacy.getGamingDiscount());
        assertEquals(new BigDecimal("80.00"), legacy.getPlayCost());
        assertEquals(new BigDecimal("0.00"), legacy.getGamingDiscountAmount());
        assertEquals(new BigDecimal("80.00"), legacy.getDiscountedPlayCost());
        assertEquals(new BigDecimal("20.00"), legacy.getProductsCost());
        assertEquals(new BigDecimal("100.00"), legacy.getFinalTotal());

        Invoice oldInvoice = new InvoiceService(() -> new ApplicationSettings(
                "Legacy Cafe", "EGP", "Thanks", null, null))
                .generate(legacy, Collections.emptyList());
        assertEquals(GamingDiscount.NONE, oldInvoice.getGamingDiscount());
        assertEquals(new BigDecimal("100.00"), oldInvoice.getTotal());
    }

    private void createRepresentativeVersionThirteenDatabase(SQLiteDatabase database)
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
                            + "VALUES (13, 'Legacy version 13')");
            statement.execute(
                    "CREATE TABLE sessions ("
                            + "id INTEGER PRIMARY KEY, station_id INTEGER NOT NULL, "
                            + "station_name TEXT NOT NULL, station_type TEXT NOT NULL, "
                            + "session_mode TEXT, station_rate_minor INTEGER NOT NULL, "
                            + "start_time TEXT NOT NULL, end_time TEXT, status TEXT NOT NULL, "
                            + "station_total_minor INTEGER NOT NULL, "
                            + "products_total_minor INTEGER NOT NULL, "
                            + "final_total_minor INTEGER NOT NULL)" );
            statement.execute(
                    "INSERT INTO sessions(id, station_id, station_name, station_type, "
                            + "session_mode, station_rate_minor, start_time, end_time, status, "
                            + "station_total_minor, products_total_minor, final_total_minor) "
                            + "VALUES (9, 3, 'Legacy Billiard', 'BILLIARD', NULL, 8000, "
                            + "'2026-08-20T18:00:00Z', '2026-08-20T19:00:00Z', "
                            + "'COMPLETED', 8000, 2000, 10000)" );
        }
    }

    private long queryForLong(SQLiteDatabase database, String sql) throws SQLException {
        try (Connection connection = database.openConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next() ? resultSet.getLong(1) : 0L;
        }
    }
}
