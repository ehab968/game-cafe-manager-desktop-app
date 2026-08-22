package com.gamecafe.gamecafemanager.data.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

final class V8CompletedSessionReportIndexMigration implements SQLiteMigration {

    @Override
    public int version() {
        return 8;
    }

    @Override
    public String description() {
        return "Index completed sessions for date reports";
    }

    @Override
    public void apply(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE INDEX sessions_completed_end_time_index "
                            + "ON sessions(status, julianday(end_time))");
        }
    }
}
