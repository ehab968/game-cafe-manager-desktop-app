package com.gamecafe.gamecafemanager.data.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.gamecafe.gamecafemanager.data.repository.SQLiteSettingsRepository;
import com.gamecafe.gamecafemanager.domain.model.ApplicationSettings;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPaperWidth;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class V12ReceiptPrintingSettingsMigrationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void preservesExistingSettingsAndAddsSafePrintingDefaults() throws SQLException {
        SQLiteDatabase database = new SQLiteDatabase(
                temporaryDirectory.resolve("legacy-v11.db"));
        createRepresentativeVersionElevenDatabase(database);

        database.initialize();

        ApplicationSettings settings = new SQLiteSettingsRepository(database).get();
        assertEquals("Legacy Cafe", settings.getCafeName());
        assertEquals("USD", settings.getCurrencyDisplay());
        assertEquals("Legacy footer", settings.getInvoiceFooter());
        assertEquals(30, settings.getMinimumSessionMinutes());
        assertEquals(15, settings.getBillingRoundingMinutes());
        assertNull(settings.getReceiptPrintSettings().getSelectedPrinterName());
        assertEquals(
                ReceiptPaperWidth.MM_80,
                settings.getReceiptPrintSettings().getPaperWidth());
        assertEquals(false, settings.getReceiptPrintSettings().isAutoPrintAfterCheckout());
        assertEquals(12, queryForInt(
                database, "SELECT MAX(version) FROM schema_migrations"));
    }

    private void createRepresentativeVersionElevenDatabase(SQLiteDatabase database)
            throws SQLException {
        try (Connection connection = database.openConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE schema_migrations ("
                            + "version INTEGER PRIMARY KEY, description TEXT NOT NULL, "
                            + "applied_at TEXT NOT NULL DEFAULT "
                            + "(strftime('%Y-%m-%dT%H:%M:%fZ', 'now')))" );
            statement.execute(
                    "INSERT INTO schema_migrations(version, description, applied_at) "
                            + "VALUES (11, 'Legacy version 11', '2026-08-28T00:00:00Z')" );
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
                            + "(1, 'Legacy Cafe', 'USD', 'Legacy footer', 30, 15, "
                            + "'2026-08-28T00:00:00Z')" );
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
