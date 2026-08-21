package com.gamecafe.gamecafemanager.data.sqlite;

import com.gamecafe.gamecafemanager.core.database.Database;
import com.gamecafe.gamecafemanager.core.database.DatabaseException;
import com.gamecafe.gamecafemanager.core.database.DatabasePaths;
import com.gamecafe.gamecafemanager.core.database.TransactionCallback;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * SQLite database implementation. It creates short-lived connections so each
 * repository operation can use try-with-resources safely.
 */
public final class SQLiteDatabase implements Database {

    private static final int BUSY_TIMEOUT_MILLISECONDS = 5_000;

    private final Path databasePath;
    private final SQLiteSchemaMigrator schemaMigrator;

    public SQLiteDatabase(Path databasePath) {
        this.databasePath = Objects.requireNonNull(databasePath, "databasePath")
                .toAbsolutePath()
                .normalize();
        this.schemaMigrator = new SQLiteSchemaMigrator();
    }

    public static SQLiteDatabase createDefault() {
        return new SQLiteDatabase(DatabasePaths.defaultDatabasePath());
    }

    @Override
    public void initialize() {
        try (Connection connection = openConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode = WAL");
            }
            schemaMigrator.migrate(connection);
        } catch (SQLException exception) {
            throw new DatabaseException(
                    "Failed to initialize SQLite database at " + databasePath,
                    exception);
        }
    }

    @Override
    public Connection openConnection() throws SQLException {
        createParentDirectory();
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);

        try {
            configureConnection(connection);
            return connection;
        } catch (SQLException exception) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }

    @Override
    public <T> T executeInTransaction(TransactionCallback<T> callback) {
        Objects.requireNonNull(callback, "callback");

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = callback.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw new DatabaseException("Database transaction failed", exception);
            } catch (RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not open database transaction", exception);
        }
    }

    public Path getDatabasePath() {
        return databasePath;
    }

    private void createParentDirectory() throws SQLException {
        Path parent = databasePath.getParent();
        if (parent == null) {
            return;
        }

        try {
            Files.createDirectories(parent);
        } catch (IOException exception) {
            throw new SQLException("Could not create database directory " + parent, exception);
        }
    }

    private void configureConnection(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = " + BUSY_TIMEOUT_MILLISECONDS);
        }
    }

    private void rollback(Connection connection, Throwable originalException) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }
}
