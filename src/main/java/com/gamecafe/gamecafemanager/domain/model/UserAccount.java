package com.gamecafe.gamecafemanager.domain.model;

import java.util.Objects;

/**
 * Internal authentication record combining public user data and its verifier.
 */
public final class UserAccount {

    private final User user;
    private final PasswordHash passwordHash;

    public UserAccount(User user, PasswordHash passwordHash) {
        this.user = Objects.requireNonNull(user, "user");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
    }

    public User getUser() {
        return user;
    }

    public PasswordHash getPasswordHash() {
        return passwordHash;
    }
}
