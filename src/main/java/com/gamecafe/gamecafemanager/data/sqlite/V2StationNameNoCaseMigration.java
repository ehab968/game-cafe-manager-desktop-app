package com.gamecafe.gamecafemanager.data.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

final class V2StationNameNoCaseMigration implements SQLiteMigration {

    @Override
    public int version() {
        return 2;
    }

    @Override
    public String description() {
        return "Enforce case-insensitive station names";
    }

    @Override
    public void apply(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE UNIQUE INDEX stations_name_nocase_unique "
                            + "ON stations(name COLLATE NOCASE)");
        }
    }
}
