package com.gamecafe.gamecafemanager.presentation.component;

import java.util.Locale;
import java.util.Objects;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

/**
 * Reusable presentation-only controls for common view states and statuses.
 */
public final class UiComponents {

    private UiComponents() {
    }

    public static Node loadingState(String message) {
        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(28.0, 28.0);
        VBox state = statePane("Loading", message);
        state.getChildren().add(0, progress);
        return state;
    }

    public static Node emptyState(String title, String message) {
        return statePane(title, message);
    }

    public static Node errorState(String title, String message, Runnable retry) {
        VBox state = statePane(title, message);
        state.getStyleClass().add("error-state");
        if (retry != null) {
            Button retryButton = new Button("Try again");
            retryButton.getStyleClass().add("primary-button");
            retryButton.setOnAction(event -> retry.run());
            state.getChildren().add(retryButton);
        }
        return state;
    }

    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>>
            statusCellFactory() {
        return column -> new TableCell<S, String>() {
            private final Label badge = new Label();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                badge.setText(item);
                badge.getStyleClass().setAll(
                        "label", "status-badge", statusStyle(item));
                setText(null);
                setGraphic(badge);
            }
        };
    }

    public static void applyStatusStyle(Label label, String status) {
        Objects.requireNonNull(label, "label");
        label.getStyleClass().setAll(
                "label", "status-badge", statusStyle(status));
    }

    private static VBox statePane(String title, String message) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empty-state-title");
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(360.0);
        messageLabel.getStyleClass().add("empty-state-message");
        VBox state = new VBox(7.0, titleLabel, messageLabel);
        state.setAlignment(Pos.CENTER);
        state.getStyleClass().add("empty-state");
        return state;
    }

    private static String statusStyle(String status) {
        String normalized = status == null
                ? ""
                : status.trim().toUpperCase(Locale.ROOT);
        switch (normalized) {
            case "ACTIVE":
            case "AVAILABLE":
            case "COMPLETED":
            case "ENABLED":
            case "RUNNING":
                return "status-positive";
            case "CANCELLED":
                return "status-negative";
            case "DISABLED":
                return "status-muted";
            default:
                return "status-accent";
        }
    }
}
