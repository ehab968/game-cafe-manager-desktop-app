package com.gamecafe.gamecafemanager.data.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(3, queryForInt("SELECT MAX(version) FROM schema_migrations"));
    }

    @Test
    void initializationIsIdempotent() throws SQLException {
        database.initialize();
        database.initialize();

        assertEquals(3, queryForInt("SELECT COUNT(*) FROM schema_migrations"));
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
