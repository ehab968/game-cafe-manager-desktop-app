package com.gamecafe.gamecafemanager.data.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

final class SQLiteSchemaMigrator {

    private static final String CREATE_MIGRATIONS_TABLE =
            "CREATE TABLE IF NOT EXISTS schema_migrations ("
                    + "version INTEGER PRIMARY KEY, "
                    + "description TEXT NOT NULL, "
                    + "applied_at TEXT NOT NULL DEFAULT "
                    + "(strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))"
                    + ")";

    private final List<SQLiteMigration> migrations =
            Arrays.asList(
                    new V1InitialSchemaMigration(),
                    new V2StationNameNoCaseMigration(),
                    new V3SessionTotalsMigration(),
                    new V4ProductNameNoCaseMigration(),
                    new V5SessionProductLineTotalMigration(),
                    new V6SessionStationTypeSnapshotMigration(),
                    new V7UsersMigration(),
                    new V8CompletedSessionReportIndexMigration(),
                    new V9ApplicationSettingsMigration(),
                    new V10RememberedAuthenticationMigration(),
                    new V11StationSessionModePricingMigration(),
                    new V12ReceiptPrintingSettingsMigration());

    void migrate(Connection connection) throws SQLException {
        createMigrationsTable(connection);
        int currentVersion = currentVersion(connection);
        int supportedVersion = migrations.get(migrations.size() - 1).version();

        if (currentVersion > supportedVersion) {
            throw new SQLException(
                    "Database schema version " + currentVersion
                            + " is newer than supported version " + supportedVersion);
        }

        for (SQLiteMigration migration : migrations) {
            if (migration.version() > currentVersion) {
                applyMigration(connection, migration);
            }
        }
    }

    private void createMigrationsTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(CREATE_MIGRATIONS_TABLE);
        }
    }

    private int currentVersion(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT COALESCE(MAX(version), 0) FROM schema_migrations")) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private void applyMigration(Connection connection, SQLiteMigration migration)
            throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try {
            migration.apply(connection);
            recordMigration(connection, migration);
            connection.commit();
        } catch (SQLException exception) {
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                exception.addSuppressed(rollbackException);
            }
            throw exception;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    private void recordMigration(Connection connection, SQLiteMigration migration)
            throws SQLException {
        String sql = "INSERT INTO schema_migrations(version, description) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, migration.version());
            statement.setString(2, migration.description());
            statement.executeUpdate();
        }
    }
}
