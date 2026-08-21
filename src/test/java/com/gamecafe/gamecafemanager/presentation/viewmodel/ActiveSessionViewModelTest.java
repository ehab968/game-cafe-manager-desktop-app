package com.gamecafe.gamecafemanager.presentation.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.model.SessionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ActiveSessionViewModelTest {

    private static final Instant START_TIME = Instant.parse("2026-08-21T12:00:00Z");

    @Test
    void recomputesElapsedTimeAfterIrregularRefreshGaps() {
        ActiveSessionViewModel viewModel = new ActiveSessionViewModel(activeSession());

        viewModel.refresh(START_TIME.plusSeconds(5L));
        assertEquals("00:00:05", viewModel.getElapsedText());

        viewModel.refresh(START_TIME.plusSeconds(7_205L));
        assertEquals("02:00:05", viewModel.getElapsedText());

        viewModel.refresh(START_TIME.plusSeconds(10L));
        assertEquals("00:00:10", viewModel.getElapsedText());
    }

    @Test
    void reconstructedViewModelUsesPersistedStartTime() {
        ActiveSessionViewModel reconstructed = new ActiveSessionViewModel(activeSession());

        reconstructed.refresh(START_TIME.plusSeconds(86_401L));

        assertEquals("24:00:01", reconstructed.getElapsedText());
    }

    @Test
    void clampsDisplayToZeroIfClockMovesBeforeStart() {
        ActiveSessionViewModel viewModel = new ActiveSessionViewModel(activeSession());

        viewModel.refresh(START_TIME.minusSeconds(30L));

        assertEquals("00:00:00", viewModel.getElapsedText());
    }

    private Session activeSession() {
        return new Session(
                42L,
                7L,
                "PlayStation Room 1",
                START_TIME,
                null,
                SessionStatus.ACTIVE,
                new BigDecimal("120.00"),
                new BigDecimal("0.00"),
                new BigDecimal("0.00"),
                new BigDecimal("0.00"));
    }
}
