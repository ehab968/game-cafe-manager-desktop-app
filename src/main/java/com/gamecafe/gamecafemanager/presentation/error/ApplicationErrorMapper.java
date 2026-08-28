package com.gamecafe.gamecafemanager.presentation.error;

import com.gamecafe.gamecafemanager.core.database.DatabaseException;
import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.domain.exception.ActiveSessionAlreadyExistsException;
import com.gamecafe.gamecafemanager.domain.exception.AuthorizationException;
import com.gamecafe.gamecafemanager.domain.exception.DuplicateCheckoutException;
import com.gamecafe.gamecafemanager.domain.exception.InsufficientStockException;
import com.gamecafe.gamecafemanager.domain.exception.InvalidCredentialsException;
import com.gamecafe.gamecafemanager.domain.exception.ProductDisabledException;
import com.gamecafe.gamecafemanager.domain.exception.ProductNotFoundException;
import com.gamecafe.gamecafemanager.domain.exception.PrinterDiscoveryException;
import com.gamecafe.gamecafemanager.domain.exception.SessionNotActiveException;
import com.gamecafe.gamecafemanager.domain.exception.SessionNotCompletedException;
import com.gamecafe.gamecafemanager.domain.exception.SessionNotFoundException;
import com.gamecafe.gamecafemanager.domain.exception.StationDisabledException;
import com.gamecafe.gamecafemanager.domain.exception.StationInUseException;
import com.gamecafe.gamecafemanager.domain.exception.StationNotFoundException;
import com.gamecafe.gamecafemanager.domain.exception.UserNotFoundException;
import java.util.Objects;

/**
 * Maps technical and domain exceptions to messages that are safe and useful in the UI.
 */
public final class ApplicationErrorMapper {

    public UserFacingError map(Throwable failure, String fallbackTitle) {
        Objects.requireNonNull(failure, "failure");
        String safeFallbackTitle = hasText(fallbackTitle)
                ? fallbackTitle.trim()
                : "Operation could not be completed";
        Throwable exception = relevantCause(failure);

        if (exception instanceof ValidationException) {
            ValidationException validation = (ValidationException) exception;
            String message = String.join(System.lineSeparator(), validation.getErrors().values());
            return error(ApplicationErrorType.VALIDATION, safeFallbackTitle,
                    hasText(message) ? message : "Check the entered values and try again.");
        }
        if (exception instanceof DatabaseException) {
            return error(
                    ApplicationErrorType.DATABASE,
                    "Database unavailable",
                    "The application could not access its data. Check that the database "
                            + "location is available, then try again.");
        }
        if (exception instanceof PrinterDiscoveryException) {
            return error(
                    ApplicationErrorType.PRINTING,
                    "Printers unavailable",
                    "Windows printer information could not be loaded. Check Windows printer "
                            + "settings and try again.");
        }
        if (exception instanceof ActiveSessionAlreadyExistsException) {
            return error(
                    ApplicationErrorType.CONFLICT,
                    "Station already running",
                    "This station already has an active session. Refresh the dashboard if needed.");
        }
        if (exception instanceof StationDisabledException) {
            return error(
                    ApplicationErrorType.CONFLICT,
                    "Station unavailable",
                    "This station is disabled. Enable it before starting a session.");
        }
        if (exception instanceof StationInUseException) {
            return error(
                    ApplicationErrorType.CONFLICT,
                    "Station is in use",
                    "Finish or cancel the active session before disabling this station.");
        }
        if (exception instanceof DuplicateCheckoutException) {
            return error(
                    ApplicationErrorType.CONFLICT,
                    "Checkout already completed",
                    "This session has already been checked out. Refresh the dashboard to see "
                            + "its current status.");
        }
        if (exception instanceof SessionNotActiveException) {
            return error(
                    ApplicationErrorType.CONFLICT,
                    "Session is not active",
                    "This action requires an active session. Refresh the dashboard and try again.");
        }
        if (exception instanceof SessionNotCompletedException) {
            return error(
                    ApplicationErrorType.CONFLICT,
                    "Session is not completed",
                    "Complete checkout before generating an invoice for this session.");
        }
        if (exception instanceof InsufficientStockException) {
            InsufficientStockException stock = (InsufficientStockException) exception;
            return error(
                    ApplicationErrorType.CONFLICT,
                    "Insufficient stock",
                    "Only " + stock.getAvailable() + " item(s) are available; "
                            + stock.getRequested() + " requested.");
        }
        if (exception instanceof ProductDisabledException) {
            return error(
                    ApplicationErrorType.CONFLICT,
                    "Product unavailable",
                    "This product is disabled and cannot be added to a session.");
        }
        if (exception instanceof AuthorizationException) {
            return error(
                    ApplicationErrorType.AUTHORIZATION,
                    "Access denied",
                    "Your account does not have permission to perform this action.");
        }
        if (exception instanceof InvalidCredentialsException) {
            return error(
                    ApplicationErrorType.AUTHORIZATION,
                    "Sign-in failed",
                    "Invalid username or password.");
        }
        if (exception instanceof StationNotFoundException
                || exception instanceof SessionNotFoundException
                || exception instanceof ProductNotFoundException
                || exception instanceof UserNotFoundException) {
            return error(
                    ApplicationErrorType.NOT_FOUND,
                    "Item no longer available",
                    "The selected item could not be found. Refresh the screen and try again.");
        }

        return error(
                ApplicationErrorType.UNEXPECTED,
                safeFallbackTitle,
                "An unexpected error occurred. Try again. If the problem continues, restart "
                        + "the application.");
    }

    private Throwable relevantCause(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null && current.getCause() != current) {
            if (isKnown(current)) {
                return current;
            }
            current = current.getCause();
        }
        return current;
    }

    private boolean isKnown(Throwable exception) {
        return exception instanceof ValidationException
                || exception instanceof DatabaseException
                || exception instanceof PrinterDiscoveryException
                || exception instanceof ActiveSessionAlreadyExistsException
                || exception instanceof StationDisabledException
                || exception instanceof StationInUseException
                || exception instanceof DuplicateCheckoutException
                || exception instanceof SessionNotActiveException
                || exception instanceof SessionNotCompletedException
                || exception instanceof InsufficientStockException
                || exception instanceof ProductDisabledException
                || exception instanceof AuthorizationException
                || exception instanceof InvalidCredentialsException
                || exception instanceof StationNotFoundException
                || exception instanceof SessionNotFoundException
                || exception instanceof ProductNotFoundException
                || exception instanceof UserNotFoundException;
    }

    private UserFacingError error(ApplicationErrorType type, String title, String message) {
        return new UserFacingError(type, title, message);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
