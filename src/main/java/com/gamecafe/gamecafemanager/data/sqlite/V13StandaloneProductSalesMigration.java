package com.gamecafe.gamecafemanager.data.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Adds auditable product-only sales without changing session history.
 */
final class V13StandaloneProductSalesMigration implements SQLiteMigration {

    @Override
    public int version() {
        return 13;
    }

    @Override
    public String description() {
        return "Add standalone product sales";
    }

    @Override
    public void apply(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE product_sales ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + "product_id INTEGER NOT NULL, "
                            + "product_name TEXT NOT NULL, "
                            + "unit_price_minor INTEGER NOT NULL "
                            + "CHECK (unit_price_minor >= 0), "
                            + "quantity INTEGER NOT NULL CHECK (quantity > 0), "
                            + "line_total_minor INTEGER NOT NULL "
                            + "CHECK (line_total_minor >= 0), "
                            + "sold_at TEXT NOT NULL, "
                            + "created_at TEXT NOT NULL DEFAULT "
                            + "(strftime('%Y-%m-%dT%H:%M:%fZ', 'now')), "
                            + "FOREIGN KEY (product_id) REFERENCES products(id) "
                            + "ON UPDATE CASCADE ON DELETE RESTRICT"
                            + ")");
            statement.execute(
                    "CREATE INDEX product_sales_product_id_index "
                            + "ON product_sales(product_id)");
            statement.execute(
                    "CREATE INDEX product_sales_sold_at_index "
                            + "ON product_sales(julianday(sold_at))");
        }
    }
}
