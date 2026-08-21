package com.gamecafe.gamecafemanager.core.database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Provides application-wide access to database lifecycle and transaction
 * boundaries. Callers must close connections returned by {@link #openConnection()}.
 */
public interface Database {

    void initialize();

    Connection openConnection() throws SQLException;

    <T> T executeInTransaction(TransactionCallback<T> callback);
}
