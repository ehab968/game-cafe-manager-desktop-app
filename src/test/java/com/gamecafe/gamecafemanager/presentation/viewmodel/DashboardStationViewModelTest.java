package com.gamecafe.gamecafemanager.presentation.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.model.SessionMode;
import com.gamecafe.gamecafemanager.domain.model.ApplicationSettings;
import com.gamecafe.gamecafemanager.domain.model.SessionStatus;
import com.gamecafe.gamecafemanager.domain.model.Station;
import com.gamecafe.gamecafemanager.domain.model.StationType;
import com.gamecafe.gamecafemanager.domain.service.PricingService;
import com.gamecafe.gamecafemanager.presentation.format.ApplicationDisplayService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DashboardStationViewModelTest {

    private static final Instant START_TIME = Instant.parse("2026-08-21T12:00:00Z");
    private final PricingService pricingService = new PricingService();
    private final ApplicationDisplayService displayService =
            new ApplicationDisplayService(ApplicationSettings::defaults);

    @Test
    void activeStationShowsRunningTimerAndCurrentGamingCost() {
        DashboardStationViewModel viewModel = new DashboardStationViewModel(
                station(true),
                activeSession(),
                displayService);

        viewModel.refresh(START_TIME.plusSeconds(5_400L), pricingService);

        assertTrue(viewModel.isActive());
        assertFalse(viewModel.canStart());
        assertEquals("Running — Multi", viewModel.getStatusText());
        assertEquals("01:30:00", viewModel.getElapsedText());
        assertEquals("EGP 240.00", viewModel.getCurrentGamingCost());
    }

    @Test
    void enabledIdleStationIsAvailableToStart() {
        DashboardStationViewModel viewModel = new DashboardStationViewModel(
                station(true), null, displayService);

        assertFalse(viewModel.isActive());
        assertTrue(viewModel.canStart());
        assertEquals("Available", viewModel.getStatusText());
        assertEquals("--", viewModel.getElapsedText());
        assertEquals("EGP 0.00", viewModel.getCurrentGamingCost());
    }

    @Test
    void disabledStationCannotStart() {
        DashboardStationViewModel viewModel = new DashboardStationViewModel(
                station(false), null, displayService);

        assertFalse(viewModel.isActive());
        assertFalse(viewModel.canStart());
        assertEquals("Disabled", viewModel.getStatusText());
    }

    private Station station(boolean enabled) {
        return new Station(
                7L,
                "PlayStation Room 1",
                StationType.PLAYSTATION,
                new BigDecimal("120.00"),
                new BigDecimal("160.00"),
                enabled);
    }

    private Session activeSession() {
        return new Session(
                42L,
                7L,
                "PlayStation Room 1",
                StationType.PLAYSTATION,
                SessionMode.MULTI,
                START_TIME,
                null,
                SessionStatus.ACTIVE,
                new BigDecimal("160.00"),
                new BigDecimal("0.00"),
                new BigDecimal("0.00"),
                new BigDecimal("0.00"));
    }
}
