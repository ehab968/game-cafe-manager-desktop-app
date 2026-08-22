package com.gamecafe.gamecafemanager.data.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

final class V4ProductNameNoCaseMigration implements SQLiteMigration {

    @Override
    public int version() {
        return 4;
    }

    @Override
    public String description() {
        return "Enforce case-insensitive product names";
    }

    @Override
    public void apply(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE UNIQUE INDEX products_name_nocase_unique "
                            + "ON products(name COLLATE NOCASE)");
        }
    }
}
