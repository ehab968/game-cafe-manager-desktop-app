package com.gamecafe.gamecafemanager.domain.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum Role {
    ADMIN(EnumSet.allOf(Permission.class)),
    CASHIER(EnumSet.of(
            Permission.OPERATE_SESSIONS,
            Permission.ADD_SESSION_PRODUCTS,
            Permission.CHECKOUT));

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = Collections.unmodifiableSet(EnumSet.copyOf(permissions));
    }

    public boolean allows(Permission permission) {
        return permissions.contains(permission);
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }
}
