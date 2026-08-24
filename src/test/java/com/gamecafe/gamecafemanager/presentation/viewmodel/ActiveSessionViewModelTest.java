package com.gamecafe.gamecafemanager.presentation.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.model.SessionMode;
import com.gamecafe.gamecafemanager.domain.model.ApplicationSettings;
import com.gamecafe.gamecafemanager.domain.model.SessionStatus;
import com.gamecafe.gamecafemanager.domain.model.StationType;
import com.gamecafe.gamecafemanager.domain.service.PricingService;
import com.gamecafe.gamecafemanager.presentation.format.ApplicationDisplayService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ActiveSessionViewModelTest {

    private static final Instant START_TIME = Instant.parse("2026-08-21T12:00:00Z");
    private final PricingService pricingService = new PricingService();
    private final ApplicationDisplayService displayService =
            new ApplicationDisplayService(ApplicationSettings::defaults);

    @Test
    void recomputesElapsedTimeAfterIrregularRefreshGaps() {
        ActiveSessionViewModel viewModel = new ActiveSessionViewModel(
                activeSession(), displayService);

        viewModel.refresh(START_TIME.plusSeconds(5L), pricingService);
        assertEquals("00:00:05", viewModel.getElapsedText());

        viewModel.refresh(START_TIME.plusSeconds(7_205L), pricingService);
        assertEquals("02:00:05", viewModel.getElapsedText());

        viewModel.refresh(START_TIME.plusSeconds(10L), pricingService);
        assertEquals("00:00:10", viewModel.getElapsedText());
    }

    @Test
    void reconstructedViewModelUsesPersistedStartTime() {
        ActiveSessionViewModel reconstructed = new ActiveSessionViewModel(
                activeSession(), displayService);

        reconstructed.refresh(START_TIME.plusSeconds(86_401L), pricingService);

        assertEquals("24:00:01", reconstructed.getElapsedText());
    }

    @Test
    void clampsDisplayToZeroIfClockMovesBeforeStart() {
        ActiveSessionViewModel viewModel = new ActiveSessionViewModel(
                activeSession(), displayService);

        viewModel.refresh(START_TIME.minusSeconds(30L), pricingService);

        assertEquals("00:00:00", viewModel.getElapsedText());
        assertEquals("EGP 0.00", viewModel.getCurrentGamingCost());
    }

    private Session activeSession() {
        return new Session(
                42L,
                7L,
                "PlayStation Room 1",
                StationType.PLAYSTATION,
                SessionMode.SINGLE,
                START_TIME,
                null,
                SessionStatus.ACTIVE,
                new BigDecimal("120.00"),
                new BigDecimal("0.00"),
                new BigDecimal("0.00"),
                new BigDecimal("0.00"));
    }
}
