package com.gamecafe.gamecafemanager.data.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class V13StandaloneProductSalesMigrationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void preservesVersionTwelveProductsAndAddsEmptySalesHistory()
            throws SQLException {
        SQLiteDatabase database = new SQLiteDatabase(
                temporaryDirectory.resolve("legacy-v12.db"));
        createRepresentativeVersionTwelveDatabase(database);

        database.initialize();

        assertEquals(14, queryForInt(
                database, "SELECT MAX(version) FROM schema_migrations"));
        assertEquals(1, queryForInt(
                database, "SELECT COUNT(*) FROM products WHERE name = 'Legacy Cola' "
                        + "AND current_price_minor = 1250 AND stock_quantity = 7"));
        assertEquals(0, queryForInt(
                database, "SELECT COUNT(*) FROM product_sales"));
        assertEquals(1, queryForInt(
                database, "SELECT COUNT(*) FROM sqlite_master WHERE type = 'index' "
                        + "AND name = 'product_sales_sold_at_index'"));
    }

    private void createRepresentativeVersionTwelveDatabase(SQLiteDatabase database)
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
                            + "VALUES (12, 'Legacy version 12')");
            statement.execute(
                    "CREATE TABLE products ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + "name TEXT NOT NULL, current_price_minor INTEGER NOT NULL, "
                            + "stock_quantity INTEGER NOT NULL, active INTEGER NOT NULL, "
                            + "created_at TEXT NOT NULL, updated_at TEXT NOT NULL)" );
            statement.execute(
                    "INSERT INTO products(id, name, current_price_minor, stock_quantity, "
                            + "active, created_at, updated_at) VALUES "
                            + "(1, 'Legacy Cola', 1250, 7, 1, "
                            + "'2026-08-28T00:00:00Z', '2026-08-28T00:00:00Z')" );
            statement.execute(
                    "CREATE TABLE sessions ("
                            + "id INTEGER PRIMARY KEY, "
                            + "station_total_minor INTEGER NOT NULL)" );
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
