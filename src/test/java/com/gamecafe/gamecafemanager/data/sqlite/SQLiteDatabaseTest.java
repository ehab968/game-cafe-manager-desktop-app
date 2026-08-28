package com.gamecafe.gamecafemanager.data.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gamecafe.gamecafemanager.core.database.DatabaseException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SQLiteDatabaseTest {

    @TempDir
    Path temporaryDirectory;

    private SQLiteDatabase database;

    @BeforeEach
    void setUp() {
        database = new SQLiteDatabase(temporaryDirectory.resolve("test.db"));
    }

    @Test
    void initializeCreatesVersionedSchema() throws SQLException {
        database.initialize();

        Set<String> tables = new HashSet<>();
        String tableQuery = "SELECT name FROM sqlite_master WHERE type = 'table'";
        try (Connection connection = database.openConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(tableQuery)) {
            while (resultSet.next()) {
                tables.add(resultSet.getString("name"));
            }
        }

        assertTrue(tables.contains("schema_migrations"));
        assertTrue(tables.contains("stations"));
        assertTrue(tables.contains("sessions"));
        assertTrue(tables.contains("products"));
        assertTrue(tables.contains("session_products"));
        assertTrue(tables.contains("users"));
        assertTrue(tables.contains("application_settings"));
        assertTrue(tables.contains("remembered_authentication"));
        assertEquals(12, queryForInt("SELECT MAX(version) FROM schema_migrations"));
        assertEquals(1, queryForInt(
                "SELECT COUNT(*) FROM pragma_table_info('stations') "
                        + "WHERE name = 'single_hourly_rate_minor'"));
        assertEquals(1, queryForInt(
                "SELECT COUNT(*) FROM pragma_table_info('stations') "
                        + "WHERE name = 'multi_hourly_rate_minor'"));
        assertEquals(1, queryForInt(
                "SELECT COUNT(*) FROM pragma_table_info('sessions') "
                        + "WHERE name = 'session_mode'"));
        assertEquals(1, queryForInt(
                "SELECT COUNT(*) FROM pragma_table_info('application_settings') "
                        + "WHERE name = 'receipt_printer_name'"));
        assertEquals(1, queryForInt(
                "SELECT COUNT(*) FROM pragma_table_info('application_settings') "
                        + "WHERE name = 'receipt_paper_width_mm'"));
        assertEquals(1, queryForInt(
                "SELECT COUNT(*) FROM pragma_table_info('application_settings') "
                        + "WHERE name = 'auto_print_receipt'"));
        assertEquals(1, queryForInt(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'index' "
                        + "AND name = 'sessions_completed_end_time_index'"));
    }

    @Test
    void initializationIsIdempotent() throws SQLException {
        database.initialize();
        database.initialize();

        assertEquals(12, queryForInt("SELECT COUNT(*) FROM schema_migrations"));
    }

    @Test
    void everyConnectionEnablesForeignKeys() throws SQLException {
        database.initialize();

        assertEquals(1, queryForInt("PRAGMA foreign_keys"));
    }

    @Test
    void transactionCommitsSuccessfulWork() throws SQLException {
        database.initialize();

        database.executeInTransaction(connection -> insertStation(connection, "Room 1"));

        assertEquals(1, queryForInt("SELECT COUNT(*) FROM stations"));
    }

    @Test
    void transactionRollsBackFailedWork() throws SQLException {
        database.initialize();

        assertThrows(IllegalStateException.class, () ->
                database.executeInTransaction(connection -> {
                    insertStation(connection, "Room 1");
                    throw new IllegalStateException("Force rollback");
                }));

        assertEquals(0, queryForInt("SELECT COUNT(*) FROM stations"));
    }

    @Test
    void initializationFailureIsNeverSilentlySwallowed() throws IOException {
        Path parentFile = Files.writeString(
                temporaryDirectory.resolve("not-a-directory"), "blocked");
        SQLiteDatabase inaccessible = new SQLiteDatabase(parentFile.resolve("database.db"));

        assertThrows(DatabaseException.class, inaccessible::initialize);
    }

    private int insertStation(Connection connection, String name) throws SQLException {
        String sql = "INSERT INTO stations(name, type, hourly_rate_minor) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, "PLAYSTATION");
            statement.setLong(3, 10_000L);
            return statement.executeUpdate();
        }
    }

    private int queryForInt(String sql) throws SQLException {
        try (Connection connection = database.openConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }
}
