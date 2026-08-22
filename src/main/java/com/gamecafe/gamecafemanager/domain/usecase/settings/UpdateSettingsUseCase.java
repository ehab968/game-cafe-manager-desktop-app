package com.gamecafe.gamecafemanager.domain.usecase.settings;

import com.gamecafe.gamecafemanager.domain.model.ApplicationSettings;
import com.gamecafe.gamecafemanager.domain.model.Permission;
import com.gamecafe.gamecafemanager.domain.service.ApplicationSettingsService;
import com.gamecafe.gamecafemanager.domain.service.ApplicationSettingsValidator;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import java.util.Objects;

public final class UpdateSettingsUseCase {

    private final ApplicationSettingsService settingsService;
    private final ApplicationSettingsValidator validator;
    private final AuthorizationService authorization;

    public UpdateSettingsUseCase(
            ApplicationSettingsService settingsService,
            ApplicationSettingsValidator validator,
            AuthorizationService authorization) {
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    public ApplicationSettings execute(ApplicationSettings settings) {
        authorization.require(Permission.MANAGE_SETTINGS);
        return settingsService.update(validator.validateAndNormalize(settings));
    }
}
