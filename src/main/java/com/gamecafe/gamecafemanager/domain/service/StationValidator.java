package com.gamecafe.gamecafemanager.domain.service;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.domain.model.StationType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Applies station business-input validation independently of JavaFX and SQLite.
 */
public final class StationValidator {

    private static final int MAXIMUM_NAME_LENGTH = 100;
    private static final int MONEY_SCALE = 2;

    public void validate(String name, StationType type, BigDecimal hourlyRate) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (name == null || name.trim().isEmpty()) {
            errors.put("name", "Name is required");
        } else if (name.trim().length() > MAXIMUM_NAME_LENGTH) {
            errors.put("name", "Name must not exceed 100 characters");
        }

        if (type == null) {
            errors.put("type", "Station type is required");
        }

        if (hourlyRate == null) {
            errors.put("hourlyRate", "Hourly price is required");
        } else if (hourlyRate.signum() <= 0) {
            errors.put("hourlyRate", "Hourly price must be greater than zero");
        } else {
            try {
                hourlyRate.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY)
                        .movePointRight(MONEY_SCALE)
                        .longValueExact();
            } catch (ArithmeticException exception) {
                errors.put(
                        "hourlyRate",
                        "Hourly price must have at most two decimal places and be within range");
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    public String normalizeName(String name) {
        return name.trim();
    }

    public BigDecimal normalizeHourlyRate(BigDecimal hourlyRate) {
        return hourlyRate.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
    }
}
