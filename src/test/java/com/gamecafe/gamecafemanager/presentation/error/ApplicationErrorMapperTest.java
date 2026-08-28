package com.gamecafe.gamecafemanager.presentation.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gamecafe.gamecafemanager.core.database.DatabaseException;
import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.domain.exception.ActiveSessionAlreadyExistsException;
import com.gamecafe.gamecafemanager.domain.exception.DuplicateCheckoutException;
import com.gamecafe.gamecafemanager.domain.exception.InsufficientStockException;
import com.gamecafe.gamecafemanager.domain.exception.PrinterDiscoveryException;
import com.gamecafe.gamecafemanager.domain.exception.SessionNotActiveException;
import com.gamecafe.gamecafemanager.domain.exception.StationInUseException;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class ApplicationErrorMapperTest {

    private final ApplicationErrorMapper mapper = new ApplicationErrorMapper();

    @Test
    void presentsAllValidationMessagesTogether() {
        ValidationException validation = ValidationException.forField(
                "quantity", "Quantity must be greater than zero");

        UserFacingError error = mapper.map(validation, "Check product details");

        assertEquals(ApplicationErrorType.VALIDATION, error.getType());
        assertEquals("Check product details", error.getTitle());
        assertEquals("Quantity must be greater than zero", error.getMessage());
    }

    @Test
    void hidesTechnicalDatabaseDetailsEvenWhenWrapped() {
        DatabaseException databaseFailure = new DatabaseException(
                "Could not execute SELECT secret FROM users",
                new SQLException("disk I/O error"));

        UserFacingError error = mapper.map(
                new IllegalStateException("startup failed", databaseFailure),
                "Application could not start");

        assertEquals(ApplicationErrorType.DATABASE, error.getType());
        assertEquals("Database unavailable", error.getTitle());
        assertFalse(error.getMessage().contains("SELECT"));
        assertFalse(error.getMessage().contains("disk I/O"));
    }

    @Test
    void mapsSessionStockAndResourceConflictsToActionableMessages() {
        assertEquals(
                "Station already running",
                mapper.map(new ActiveSessionAlreadyExistsException(4L), "Start failed")
                        .getTitle());
        assertEquals(
                "Session is not active",
                mapper.map(new SessionNotActiveException(10L), "Finish failed").getTitle());
        assertEquals(
                "Checkout already completed",
                mapper.map(new DuplicateCheckoutException(10L), "Finish failed").getTitle());
        assertEquals(
                "Station is in use",
                mapper.map(new StationInUseException(4L), "Disable failed").getTitle());

        UserFacingError stock = mapper.map(
                new InsufficientStockException(2L, 5, 3), "Add product failed");
        assertEquals("Insufficient stock", stock.getTitle());
        assertTrue(stock.getMessage().contains("3"));
        assertTrue(stock.getMessage().contains("5"));
    }

    @Test
    void unexpectedFailureUsesSafeFallbackWithoutLeakingItsMessage() {
        UserFacingError error = mapper.map(
                new IllegalStateException("password=secret"), "Could not save station");

        assertEquals(ApplicationErrorType.UNEXPECTED, error.getType());
        assertEquals("Could not save station", error.getTitle());
        assertFalse(error.getMessage().contains("secret"));
    }

    @Test
    void printerDiscoveryFailureUsesSafePrinterGuidance() {
        UserFacingError error = mapper.map(
                new PrinterDiscoveryException(
                        "spooler internals", new IllegalStateException("secret device path")),
                "Could not discover printers");

        assertEquals(ApplicationErrorType.PRINTING, error.getType());
        assertEquals("Printers unavailable", error.getTitle());
        assertFalse(error.getMessage().contains("secret"));
        assertFalse(error.getMessage().contains("spooler"));
    }
}
