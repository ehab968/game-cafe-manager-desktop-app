package com.gamecafe.gamecafemanager.data.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Adds optional receipt-print configuration without changing existing settings
 * or transaction history.
 */
final class V12ReceiptPrintingSettingsMigration implements SQLiteMigration {

    @Override
    public int version() {
        return 12;
    }

    @Override
    public String description() {
        return "Add receipt printer settings";
    }

    @Override
    public void apply(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "ALTER TABLE application_settings ADD COLUMN "
                            + "receipt_printer_name TEXT");
            statement.execute(
                    "ALTER TABLE application_settings ADD COLUMN "
                            + "receipt_paper_width_mm INTEGER NOT NULL DEFAULT 80 "
                            + "CHECK (receipt_paper_width_mm IN (58, 80))");
            statement.execute(
                    "ALTER TABLE application_settings ADD COLUMN "
                            + "auto_print_receipt INTEGER NOT NULL DEFAULT 0 "
                            + "CHECK (auto_print_receipt IN (0, 1))");
        }
    }
}
