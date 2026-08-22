package com.gamecafe.gamecafemanager.domain.repository;

import com.gamecafe.gamecafemanager.domain.model.ApplicationSettings;

public interface SettingsRepository {

    ApplicationSettings get();

    ApplicationSettings update(ApplicationSettings settings);
}
