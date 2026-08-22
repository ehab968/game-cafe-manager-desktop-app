package com.gamecafe.gamecafemanager.domain.exception;

import com.gamecafe.gamecafemanager.domain.model.Permission;

public final class AuthorizationException extends RuntimeException {

    public AuthorizationException(Permission permission) {
        super("Permission denied: " + permission.name());
    }

    public AuthorizationException(String message) {
        super(message);
    }
}
