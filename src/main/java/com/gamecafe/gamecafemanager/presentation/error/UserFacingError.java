package com.gamecafe.gamecafemanager.presentation.error;

import java.util.Objects;

/**
 * Safe error details that can be shown to an application user.
 */
public final class UserFacingError {

    private final ApplicationErrorType type;
    private final String title;
    private final String message;

    public UserFacingError(ApplicationErrorType type, String title, String message) {
        this.type = Objects.requireNonNull(type, "type");
        this.title = Objects.requireNonNull(title, "title");
        this.message = Objects.requireNonNull(message, "message");
    }

    public ApplicationErrorType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }
}
