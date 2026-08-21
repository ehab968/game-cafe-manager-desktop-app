package com.gamecafe.gamecafemanager.presentation.controller;

import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.usecase.session.GetActiveSessionsUseCase;
import com.gamecafe.gamecafemanager.presentation.viewmodel.ActiveSessionViewModel;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Duration;

/**
 * Refreshes timer presentation once per second. Timeline ticks never mutate or
 * accumulate elapsed time; each tick recomputes it from Clock and startTime.
 */
public class ActiveSessionsController {

    private static final DateTimeFormatter START_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private final GetActiveSessionsUseCase getActiveSessionsUseCase;
    private final Clock clock;
    private final ObservableList<ActiveSessionViewModel> sessions =
            FXCollections.observableArrayList();

    @FXML
    private TableView<ActiveSessionViewModel> activeSessionsTable;

    @FXML
    private TableColumn<ActiveSessionViewModel, String> stationColumn;

    @FXML
    private TableColumn<ActiveSessionViewModel, String> startedColumn;

    @FXML
    private TableColumn<ActiveSessionViewModel, String> hourlyRateColumn;

    @FXML
    private TableColumn<ActiveSessionViewModel, String> elapsedColumn;

    @FXML
    private TableColumn<ActiveSessionViewModel, String> statusColumn;

    private Timeline refreshTimeline;

    public ActiveSessionsController(GetActiveSessionsUseCase getActiveSessionsUseCase, Clock clock) {
        this.getActiveSessionsUseCase = Objects.requireNonNull(
                getActiveSessionsUseCase, "getActiveSessionsUseCase");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @FXML
    private void initialize() {
        stationColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getStationName()));
        startedColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(START_TIME_FORMAT.format(cell.getValue().getStartTime())));
        hourlyRateColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().getHourlyRateSnapshot().toPlainString()));
        elapsedColumn.setCellValueFactory(cell -> cell.getValue().elapsedTextProperty());
        statusColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper("Running"));
        activeSessionsTable.setItems(sessions);

        reloadSessions();
        refreshTimeline = new Timeline(new KeyFrame(
                Duration.seconds(1.0),
                event -> refreshElapsedTimes()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    @FXML
    public void reloadSessions() {
        try {
            sessions.clear();
            for (Session session : getActiveSessionsUseCase.execute()) {
                sessions.add(new ActiveSessionViewModel(session));
            }
            refreshElapsedTimes();
        } catch (RuntimeException exception) {
            showError("Could not load active sessions", exception.getMessage());
        }
    }

    private void refreshElapsedTimes() {
        Instant currentTime = clock.instant();
        for (ActiveSessionViewModel session : sessions) {
            session.refresh(currentTime);
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Game Cafe Manager");
        alert.setHeaderText(title);
        alert.setContentText(message == null || message.trim().isEmpty()
                ? "The operation could not be completed."
                : message);
        if (activeSessionsTable.getScene() != null) {
            alert.initOwner(activeSessionsTable.getScene().getWindow());
        }
        alert.showAndWait();
    }
}
