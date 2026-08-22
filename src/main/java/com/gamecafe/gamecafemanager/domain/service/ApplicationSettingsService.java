package com.gamecafe.gamecafemanager.domain.service;

import com.gamecafe.gamecafemanager.domain.model.ApplicationSettings;
import com.gamecafe.gamecafemanager.domain.repository.SettingsRepository;
import java.util.Objects;

/**
 * Keeps the process-wide settings snapshot synchronized with SQLite updates.
 */
public final class ApplicationSettingsService implements SettingsProvider {

    private final SettingsRepository repository;
    private ApplicationSettings currentSettings;

    public ApplicationSettingsService(SettingsRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
        currentSettings = repository.get();
    }

    @Override
    public synchronized ApplicationSettings getSettings() {
        return currentSettings;
    }

    public synchronized ApplicationSettings update(ApplicationSettings settings) {
        currentSettings = repository.update(
                Objects.requireNonNull(settings, "settings"));
        return currentSettings;
    }
}
