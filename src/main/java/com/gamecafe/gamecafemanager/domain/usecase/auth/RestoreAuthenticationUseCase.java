package com.gamecafe.gamecafemanager.domain.usecase.auth;

import com.gamecafe.gamecafemanager.domain.model.User;
import com.gamecafe.gamecafemanager.domain.service.AuthenticationService;
import java.util.Objects;
import java.util.Optional;

/**
 * Restores a previously authenticated enabled user without handling UI state.
 */
public final class RestoreAuthenticationUseCase {

    private final AuthenticationService authenticationService;

    public RestoreAuthenticationUseCase(AuthenticationService authenticationService) {
        this.authenticationService = Objects.requireNonNull(
                authenticationService, "authenticationService");
    }

    public Optional<User> execute() {
        return authenticationService.restoreRememberedUser();
    }
}
