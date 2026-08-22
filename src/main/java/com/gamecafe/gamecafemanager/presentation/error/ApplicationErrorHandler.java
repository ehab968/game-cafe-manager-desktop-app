package com.gamecafe.gamecafemanager.presentation.error;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.gamecafe.gamecafemanager.presentation.style.UiStyles;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Window;

/**
 * Reports failures and presents their safe representation in JavaFX.
 */
public final class ApplicationErrorHandler {

    private static final Logger LOGGER =
            Logger.getLogger(ApplicationErrorHandler.class.getName());

    private final ApplicationErrorMapper mapper;
    private final Supplier<String> applicationName;

    public ApplicationErrorHandler(
            ApplicationErrorMapper mapper,
            Supplier<String> applicationName) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.applicationName = Objects.requireNonNull(applicationName, "applicationName");
    }

    public UserFacingError handle(Throwable failure, String fallbackTitle) {
        UserFacingError error = mapper.map(failure, fallbackTitle);
        Level level = error.getType() == ApplicationErrorType.DATABASE
                        || error.getType() == ApplicationErrorType.UNEXPECTED
                ? Level.SEVERE
                : Level.WARNING;
        LOGGER.log(level, error.getTitle() + ": " + error.getMessage(), failure);
        return error;
    }

    public void show(Window owner, String fallbackTitle, Throwable failure) {
        UserFacingError error = handle(failure, fallbackTitle);
        Runnable display = () -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            UiStyles.apply(alert.getDialogPane());
            alert.setTitle(safeApplicationName());
            alert.setHeaderText(error.getTitle());
            alert.setContentText(error.getMessage());
            if (owner != null) {
                alert.initOwner(owner);
            }
            alert.showAndWait();
        };
        if (Platform.isFxApplicationThread()) {
            display.run();
        } else {
            Platform.runLater(display);
        }
    }

    private String safeApplicationName() {
        String name = applicationName.get();
        return name == null || name.trim().isEmpty() ? "Application" : name.trim();
    }
}
