package com.gamecafe.gamecafemanager.domain.service;

import com.gamecafe.gamecafemanager.domain.exception.AuthorizationException;
import com.gamecafe.gamecafemanager.domain.model.Permission;
import com.gamecafe.gamecafemanager.domain.model.User;
import java.util.Objects;

public final class AuthorizationService {

    private final AuthenticationService authenticationService;

    public AuthorizationService(AuthenticationService authenticationService) {
        this.authenticationService = Objects.requireNonNull(
                authenticationService, "authenticationService");
    }

    public User requireAuthenticatedUser() {
        return authenticationService.getCurrentUser()
                .orElseThrow(() -> new AuthorizationException("Authentication is required"));
    }

    public void require(Permission permission) {
        User user = requireAuthenticatedUser();
        if (!user.getRole().allows(permission)) {
            throw new AuthorizationException(permission);
        }
    }

    public boolean isAllowed(Permission permission) {
        return authenticationService.getCurrentUser()
                .map(user -> user.getRole().allows(permission))
                .orElse(false);
    }
}
