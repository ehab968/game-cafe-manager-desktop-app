package com.gamecafe.gamecafemanager.domain.model;

import java.util.Objects;

public final class User {

    private final Long id;
    private final String username;
    private final Role role;
    private final boolean enabled;

    public User(Long id, String username, Role role, boolean enabled) {
        this.id = id;
        this.username = Objects.requireNonNull(username, "username");
        this.role = Objects.requireNonNull(role, "role");
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
