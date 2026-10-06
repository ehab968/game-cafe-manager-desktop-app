package com.gamecafe.gamecafemanager.presentation.controller;

import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.service.PricingService;
import com.gamecafe.gamecafemanager.domain.usecase.session.GetActiveSessionsUseCase;
import com.gamecafe.gamecafemanager.presentation.component.UiComponents;
import com.gamecafe.gamecafemanager.presentation.error.ApplicationErrorHandler;
import com.gamecafe.gamecafemanager.presentation.timer.SessionDisplayTicker;
import com.gamecafe.gamecafemanager.presentation.viewmodel.ActiveSessionViewModel;
import com.gamecafe.gamecafemanager.presentation.format.ApplicationDisplayService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Refreshes timer presentation from the shared display ticker. Ticks never
 * mutate or accumulate elapsed time; each refresh recomputes it from Clock and
 * startTime.
 */
public class ActiveSessionsController {

    private static final DateTimeFormatter START_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private final GetActiveSessionsUseCase getActiveSessionsUseCase;
    private final Clock clock;
    private final PricingService pricingService;
    private final ApplicationDisplayService displayService;
    private final ApplicationErrorHandler errorHandler;
    private final SessionDisplayTicker sessionDisplayTicker;
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

    private boolean viewActive;

    public ActiveSessionsController(
            GetActiveSessionsUseCase getActiveSessionsUseCase,
            Clock clock,
            PricingService pricingService,
            ApplicationDisplayService displayService,
            ApplicationErrorHandler errorHandler,
            SessionDisplayTicker sessionDisplayTicker) {
        this.getActiveSessionsUseCase = Objects.requireNonNull(
                getActiveSessionsUseCase, "getActiveSessionsUseCase");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.pricingService = Objects.requireNonNull(pricingService, "pricingService");
        this.displayService = Objects.requireNonNull(displayService, "displayService");
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
        this.sessionDisplayTicker = Objects.requireNonNull(
                sessionDisplayTicker, "sessionDisplayTicker");
    }

    @FXML
    private void initialize() {
        activeSessionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        stationColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getStationName()));
        startedColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(START_TIME_FORMAT.format(cell.getValue().getStartTime())));
        hourlyRateColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                displayService.formatMoney(cell.getValue().getHourlyRateSnapshot())));
        elapsedColumn.setCellValueFactory(cell -> cell.getValue().elapsedTextProperty());
        statusColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper("Running"));
        statusColumn.setCellFactory(UiComponents.statusCellFactory());
        activeSessionsTable.setItems(sessions);

        reloadSessions();
    }

    @FXML
    public void reloadSessions() {
        sessions.clear();
        activeSessionsTable.setPlaceholder(UiComponents.loadingState("Loading active sessions…"));
        try {
            for (Session session : getActiveSessionsUseCase.execute()) {
                sessions.add(new ActiveSessionViewModel(session, displayService));
            }
            activeSessionsTable.setPlaceholder(UiComponents.emptyState(
                    "No active sessions",
                    "Start a station from the dashboard and it will appear here."));
            refreshElapsedTimes();
            updateTickerState();
        } catch (RuntimeException exception) {
            updateTickerState();
            activeSessionsTable.setPlaceholder(UiComponents.errorState(
                    "Active sessions unavailable",
                    "Running sessions could not be loaded.",
                    this::reloadSessions));
            showError("Could not load active sessions", exception);
        }
    }

    private void refreshElapsedTimes() {
        Instant currentTime = clock.instant();
        for (ActiveSessionViewModel session : sessions) {
            session.refresh(currentTime, pricingService);
        }
    }

    public void activate() {
        viewActive = true;
        refreshElapsedTimes();
        updateTickerState();
    }

    public void deactivate() {
        if (!viewActive) {
            return;
        }
        viewActive = false;
        sessionDisplayTicker.stop();
    }

    private void updateTickerState() {
        if (!viewActive) {
            return;
        }
        if (sessions.isEmpty()) {
            sessionDisplayTicker.stop();
        } else {
            sessionDisplayTicker.start(this::refreshElapsedTimes);
        }
    }

    private void showError(String title, Throwable failure) {
        errorHandler.show(
                activeSessionsTable.getScene() == null
                        ? null
                        : activeSessionsTable.getScene().getWindow(),
                title,
                failure);
    }

    public void dispose() {
        deactivate();
    }
}
