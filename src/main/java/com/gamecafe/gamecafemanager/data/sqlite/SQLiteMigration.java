package com.gamecafe.gamecafemanager.data.sqlite;

import java.sql.Connection;
import java.sql.SQLException;

interface SQLiteMigration {

    int version();

    String description();

    void apply(Connection connection) throws SQLException;
}
