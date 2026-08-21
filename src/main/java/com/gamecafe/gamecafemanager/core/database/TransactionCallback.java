package com.gamecafe.gamecafemanager.core.database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Work executed atomically using a single database connection.
 */
@FunctionalInterface
public interface TransactionCallback<T> {

    T execute(Connection connection) throws SQLException;
}
