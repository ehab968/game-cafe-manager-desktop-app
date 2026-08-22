package com.gamecafe.gamecafemanager.domain.service;

import com.gamecafe.gamecafemanager.domain.model.ApplicationSettings;

@FunctionalInterface
public interface SettingsProvider {

    ApplicationSettings getSettings();
}
