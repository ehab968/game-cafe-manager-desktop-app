package com.gamecafe.gamecafemanager.presentation.viewmodel;

import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.model.SessionMode;
import com.gamecafe.gamecafemanager.domain.model.Station;
import com.gamecafe.gamecafemanager.domain.service.PricingService;
import com.gamecafe.gamecafemanager.presentation.format.ApplicationDisplayService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

/**
 * Presentation state for one dashboard card. Active timing and pricing are
 * delegated to ActiveSessionViewModel and PricingService.
 */
public final class DashboardStationViewModel {

    private final Station station;
    private final Session activeSession;
    private final ActiveSessionViewModel activeSessionViewModel;
    private final ReadOnlyStringWrapper elapsedText = new ReadOnlyStringWrapper("--");
    private final ReadOnlyStringWrapper currentGamingCost = new ReadOnlyStringWrapper();

    public DashboardStationViewModel(
            Station station,
            Session activeSession,
            ApplicationDisplayService displayService) {
        this.station = Objects.requireNonNull(station, "station");
        ApplicationDisplayService display = Objects.requireNonNull(
                displayService, "displayService");
        currentGamingCost.set(display.formatMoney(BigDecimal.ZERO.setScale(2)));
        this.activeSession = activeSession;
        if (activeSession != null && activeSession.getStationId() != station.getId()) {
            throw new IllegalArgumentException("Active session belongs to a different station");
        }
        this.activeSessionViewModel = activeSession == null
                ? null
                : new ActiveSessionViewModel(activeSession, display);
    }

    public void refresh(Instant currentTime, PricingService pricingService) {
        if (activeSessionViewModel == null) {
            return;
        }
        activeSessionViewModel.refresh(currentTime, pricingService);
        elapsedText.set(activeSessionViewModel.getElapsedText());
        currentGamingCost.set(activeSessionViewModel.getCurrentGamingCost());
    }

    public long getStationId() {
        return station.getId();
    }

    public String getStationName() {
        return station.getName();
    }

    public String getStationType() {
        return station.getType().getDisplayName();
    }

    public String getStatusText() {
        if (isActive()) {
            return activeSession.getMode() == null
                    ? "Running"
                    : "Running — " + activeSession.getMode().getDisplayName();
        }
        return station.isEnabled() ? "Available" : "Disabled";
    }

    public boolean isActive() {
        return activeSession != null;
    }

    public boolean canStart() {
        return station.isEnabled() && !isActive();
    }

    public boolean supportsSessionModes() {
        return station.getType().supportsSessionModes();
    }

    public BigDecimal getSingleHourlyRate() {
        return station.getSingleHourlyRate();
    }

    public BigDecimal getMultiHourlyRate() {
        return station.getMultiHourlyRate();
    }

    public SessionMode getActiveSessionMode() {
        return activeSession == null ? null : activeSession.getMode();
    }

    public long getActiveSessionId() {
        if (activeSession == null || activeSession.getId() == null) {
            throw new IllegalStateException("Station does not have an active session");
        }
        return activeSession.getId();
    }

    public String getElapsedText() {
        return elapsedText.get();
    }

    public ReadOnlyStringProperty elapsedTextProperty() {
        return elapsedText.getReadOnlyProperty();
    }

    public String getCurrentGamingCost() {
        return currentGamingCost.get();
    }

    public ReadOnlyStringProperty currentGamingCostProperty() {
        return currentGamingCost.getReadOnlyProperty();
    }
}
