package com.gamecafe.gamecafemanager.presentation.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.util.List;
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

    @Test
    void refreshesMultipleSingleMultiAndBilliardSessionsFromOneTimeSnapshot() {
        List<DashboardStationViewModel> viewModels = List.of(
                activeViewModel(
                        1L, StationType.PLAYSTATION, SessionMode.SINGLE, "60.00"),
                activeViewModel(
                        2L, StationType.PING_PONG, SessionMode.MULTI, "80.00"),
                activeViewModel(
                        3L, StationType.BILLIARD, null, "50.00"));

        Instant sharedCurrentTime = START_TIME.plusSeconds(5_400L);
        viewModels.forEach(viewModel ->
                viewModel.refresh(sharedCurrentTime, pricingService));

        assertEquals("EGP 90.00", viewModels.get(0).getCurrentGamingCost());
        assertEquals(SessionMode.SINGLE, viewModels.get(0).getActiveSessionMode());
        assertEquals("EGP 120.00", viewModels.get(1).getCurrentGamingCost());
        assertEquals(SessionMode.MULTI, viewModels.get(1).getActiveSessionMode());
        assertEquals("EGP 75.00", viewModels.get(2).getCurrentGamingCost());
        assertNull(viewModels.get(2).getActiveSessionMode());
        viewModels.forEach(viewModel ->
                assertEquals("01:30:00", viewModel.getElapsedText()));
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

    private DashboardStationViewModel activeViewModel(
            long id,
            StationType type,
            SessionMode mode,
            String hourlyRate) {
        BigDecimal rate = new BigDecimal(hourlyRate);
        Station station = type.supportsSessionModes()
                ? new Station(id, "Station " + id, type, rate, rate, true)
                : new Station(id, "Station " + id, type, rate, true);
        Session session = new Session(
                id,
                id,
                station.getName(),
                type,
                mode,
                START_TIME,
                null,
                SessionStatus.ACTIVE,
                rate,
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2));
        return new DashboardStationViewModel(station, session, displayService);
    }
}
