package com.gamecafe.gamecafemanager.domain.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.domain.model.StationType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class StationValidatorTest {

    private final StationValidator validator = new StationValidator();

    @Test
    void acceptsValidStationDetails() {
        assertDoesNotThrow(() -> validator.validate(
                "PlayStation Room 1",
                StationType.PLAYSTATION,
                new BigDecimal("125.50"),
                new BigDecimal("160.00")));
    }

    @Test
    void rejectsBlankName() {
        assertThrows(ValidationException.class, () -> validator.validate(
                "   ",
                StationType.BILLIARD,
                new BigDecimal("50.00")));
    }

    @Test
    void rejectsMissingStationType() {
        assertThrows(ValidationException.class, () -> validator.validate(
                "Table 1",
                null,
                new BigDecimal("50.00")));
    }

    @Test
    void acceptsZeroModePrices() {
        assertDoesNotThrow(() -> validator.validate(
                "Table 1",
                StationType.PING_PONG,
                BigDecimal.ZERO,
                BigDecimal.ZERO));
    }

    @Test
    void rejectsNegativeModePrice() {
        assertThrows(ValidationException.class, () -> validator.validate(
                "Table 1",
                StationType.PING_PONG,
                BigDecimal.ZERO,
                new BigDecimal("-1.00")));
    }

    @Test
    void requiresBothPricesForModeCapableStation() {
        assertThrows(ValidationException.class, () -> validator.validate(
                "Room 1",
                StationType.PLAYSTATION,
                new BigDecimal("60.00")));
    }

    @Test
    void billiardStillRequiresPositiveHourlyPrice() {
        assertThrows(ValidationException.class, () -> validator.validate(
                "Billiard 1",
                StationType.BILLIARD,
                BigDecimal.ZERO));
    }

    @Test
    void rejectsFractionalMinorUnits() {
        assertThrows(ValidationException.class, () -> validator.validate(
                "Table 1",
                StationType.PING_PONG,
                new BigDecimal("10.001"),
                new BigDecimal("20.00")));
    }
}
