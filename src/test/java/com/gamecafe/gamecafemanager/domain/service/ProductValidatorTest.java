package com.gamecafe.gamecafemanager.domain.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductValidatorTest {

    private final ProductValidator validator = new ProductValidator();

    @Test
    void acceptsValidProductIncludingZeroValues() {
        assertDoesNotThrow(() -> validator.validate("Water", BigDecimal.ZERO, 0));
    }

    @Test
    void rejectsBlankName() {
        assertThrows(ValidationException.class, () ->
                validator.validate("  ", new BigDecimal("10.00"), 2));
    }

    @Test
    void rejectsNegativePrice() {
        assertThrows(ValidationException.class, () ->
                validator.validate("Water", new BigDecimal("-0.01"), 2));
    }

    @Test
    void rejectsFractionalMinorUnits() {
        assertThrows(ValidationException.class, () ->
                validator.validate("Water", new BigDecimal("10.001"), 2));
    }

    @Test
    void rejectsNegativeStock() {
        assertThrows(ValidationException.class, () ->
                validator.validate("Water", new BigDecimal("10.00"), -1));
        assertThrows(ValidationException.class, () -> validator.validateStock(-1));
    }
}
