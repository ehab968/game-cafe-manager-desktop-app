package com.gamecafe.gamecafemanager.core.database;

/**
 * Indicates that database infrastructure could not complete an operation.
 */
public class DatabaseException extends RuntimeException {

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
