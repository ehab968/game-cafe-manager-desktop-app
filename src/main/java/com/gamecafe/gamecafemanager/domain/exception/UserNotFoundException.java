package com.gamecafe.gamecafemanager.domain.exception;

public final class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(long userId) {
        super("User " + userId + " was not found");
    }
}
