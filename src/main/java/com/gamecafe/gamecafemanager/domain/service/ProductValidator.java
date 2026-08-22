package com.gamecafe.gamecafemanager.domain.service;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Applies product business-input validation independently of JavaFX and SQLite.
 */
public final class ProductValidator {

    private static final int MAXIMUM_NAME_LENGTH = 100;
    private static final int MONEY_SCALE = 2;

    public void validate(String name, BigDecimal currentPrice, int stockQuantity) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (name == null || name.trim().isEmpty()) {
            errors.put("name", "Name is required");
        } else if (name.trim().length() > MAXIMUM_NAME_LENGTH) {
            errors.put("name", "Name must not exceed 100 characters");
        }

        validatePrice(currentPrice, errors);
        if (stockQuantity < 0) {
            errors.put("stockQuantity", "Stock quantity must be zero or greater");
        }

        throwIfInvalid(errors);
    }

    public void validateStock(int stockQuantity) {
        if (stockQuantity < 0) {
            throw ValidationException.forField(
                    "stockQuantity", "Stock quantity must be zero or greater");
        }
    }

    public String normalizeName(String name) {
        return name.trim();
    }

    public BigDecimal normalizeCurrentPrice(BigDecimal currentPrice) {
        return currentPrice.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
    }

    private void validatePrice(BigDecimal currentPrice, Map<String, String> errors) {
        if (currentPrice == null) {
            errors.put("currentPrice", "Current price is required");
        } else if (currentPrice.signum() < 0) {
            errors.put("currentPrice", "Current price must be zero or greater");
        } else {
            try {
                currentPrice.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY)
                        .movePointRight(MONEY_SCALE)
                        .longValueExact();
            } catch (ArithmeticException exception) {
                errors.put(
                        "currentPrice",
                        "Current price must have at most two decimal places and be within range");
            }
        }
    }

    private void throwIfInvalid(Map<String, String> errors) {
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
