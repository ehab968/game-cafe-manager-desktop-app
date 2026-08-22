package com.gamecafe.gamecafemanager.data.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

final class V6SessionStationTypeSnapshotMigration implements SQLiteMigration {

    @Override
    public int version() {
        return 6;
    }

    @Override
    public String description() {
        return "Snapshot station type on sessions";
    }

    @Override
    public void apply(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "ALTER TABLE sessions ADD COLUMN station_type TEXT "
                            + "CHECK (station_type IN ('PLAYSTATION', 'BILLIARD', 'PING_PONG'))");
            statement.execute(
                    "UPDATE sessions SET station_type = ("
                            + "SELECT stations.type FROM stations WHERE stations.id = sessions.station_id"
                            + ")");
        }
    }
}
