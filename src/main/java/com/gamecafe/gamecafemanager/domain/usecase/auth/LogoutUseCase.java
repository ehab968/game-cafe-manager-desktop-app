package com.gamecafe.gamecafemanager.domain.usecase.auth;

import com.gamecafe.gamecafemanager.domain.service.AuthenticationService;
import java.util.Objects;

public final class LogoutUseCase {

    private final AuthenticationService authenticationService;

    public LogoutUseCase(AuthenticationService authenticationService) {
        this.authenticationService = Objects.requireNonNull(
                authenticationService, "authenticationService");
    }

    public void execute() {
        authenticationService.logout();
    }
}
