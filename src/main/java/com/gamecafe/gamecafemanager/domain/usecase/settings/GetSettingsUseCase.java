package com.gamecafe.gamecafemanager.domain.usecase.settings;

import com.gamecafe.gamecafemanager.domain.model.ApplicationSettings;
import com.gamecafe.gamecafemanager.domain.model.Permission;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import com.gamecafe.gamecafemanager.domain.service.SettingsProvider;
import java.util.Objects;

public final class GetSettingsUseCase {

    private final SettingsProvider settingsProvider;
    private final AuthorizationService authorization;

    public GetSettingsUseCase(
            SettingsProvider settingsProvider,
            AuthorizationService authorization) {
        this.settingsProvider = Objects.requireNonNull(settingsProvider, "settingsProvider");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    public ApplicationSettings execute() {
        authorization.require(Permission.MANAGE_SETTINGS);
        return settingsProvider.getSettings();
    }
}
