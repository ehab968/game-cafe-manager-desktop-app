package com.gamecafe.gamecafemanager.domain.usecase.auth;

import com.gamecafe.gamecafemanager.domain.model.User;
import com.gamecafe.gamecafemanager.domain.service.AuthenticationService;
import java.util.Objects;

public final class LoginUseCase {

    private final AuthenticationService authenticationService;

    public LoginUseCase(AuthenticationService authenticationService) {
        this.authenticationService = Objects.requireNonNull(
                authenticationService, "authenticationService");
    }

    public User execute(String username, char[] password) {
        return authenticationService.authenticate(username, password);
    }
}
