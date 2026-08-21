package com.gamecafe.gamecafemanager.presentation.controller;

import com.gamecafe.gamecafemanager.core.navigation.NavigationTarget;
import com.gamecafe.gamecafemanager.domain.usecase.session.GetActiveSessionsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.CreateStationUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.GetStationsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.SetStationEnabledUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.UpdateStationUseCase;
import java.io.IOException;
import java.time.Clock;
import java.util.Objects;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controls the application shell and its foundation-level navigation.
 */
public class MainController {

    private static final String STATIONS_VIEW =
            "/com/gamecafe/gamecafemanager/presentation/view/stations-view.fxml";
    private static final String ACTIVE_SESSIONS_VIEW =
            "/com/gamecafe/gamecafemanager/presentation/view/active-sessions-view.fxml";

    private final CreateStationUseCase createStationUseCase;
    private final UpdateStationUseCase updateStationUseCase;
    private final GetStationsUseCase getStationsUseCase;
    private final SetStationEnabledUseCase setStationEnabledUseCase;
    private final GetActiveSessionsUseCase getActiveSessionsUseCase;
    private final Clock clock;

    @FXML
    private VBox informationPage;

    @FXML
    private StackPane contentArea;

    @FXML
    private Label pageTitle;

    @FXML
    private Label pageDescription;

    private Parent stationsView;
    private StationsController stationsController;
    private Parent activeSessionsView;
    private ActiveSessionsController activeSessionsController;

    public MainController(
            CreateStationUseCase createStationUseCase,
            UpdateStationUseCase updateStationUseCase,
            GetStationsUseCase getStationsUseCase,
            SetStationEnabledUseCase setStationEnabledUseCase,
            GetActiveSessionsUseCase getActiveSessionsUseCase,
            Clock clock) {
        this.createStationUseCase = Objects.requireNonNull(createStationUseCase, "createStationUseCase");
        this.updateStationUseCase = Objects.requireNonNull(updateStationUseCase, "updateStationUseCase");
        this.getStationsUseCase = Objects.requireNonNull(getStationsUseCase, "getStationsUseCase");
        this.setStationEnabledUseCase = Objects.requireNonNull(
                setStationEnabledUseCase, "setStationEnabledUseCase");
        this.getActiveSessionsUseCase = Objects.requireNonNull(
                getActiveSessionsUseCase, "getActiveSessionsUseCase");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @FXML
    private void initialize() {
        navigateTo(NavigationTarget.ACTIVE_SESSIONS);
    }

    @FXML
    private void showWelcome() {
        navigateTo(NavigationTarget.WELCOME);
    }

    @FXML
    private void showStations() {
        navigateTo(NavigationTarget.STATIONS);
    }

    @FXML
    private void showActiveSessions() {
        navigateTo(NavigationTarget.ACTIVE_SESSIONS);
    }

    @FXML
    private void showAbout() {
        navigateTo(NavigationTarget.ABOUT);
    }

    private void navigateTo(NavigationTarget target) {
        if (target == NavigationTarget.STATIONS) {
            showStationsView();
            return;
        }
        if (target == NavigationTarget.ACTIVE_SESSIONS) {
            showActiveSessionsView();
            return;
        }

        hideFeatureViews();
        informationPage.setVisible(true);
        informationPage.setManaged(true);
        pageTitle.setText(target.getTitle());
        pageDescription.setText(target.getDescription());
    }

    private void showStationsView() {
        if (stationsView == null) {
            stationsView = loadStationsView();
            contentArea.getChildren().add(stationsView);
        } else {
            stationsController.refresh();
        }
        hideFeatureViews();
        informationPage.setVisible(false);
        informationPage.setManaged(false);
        stationsView.setManaged(true);
        stationsView.setVisible(true);
    }

    private Parent loadStationsView() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(STATIONS_VIEW));
        stationsController = new StationsController(
                createStationUseCase,
                updateStationUseCase,
                getStationsUseCase,
                setStationEnabledUseCase,
                getActiveSessionsUseCase);
        loader.setController(stationsController);
        try {
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load Stations view", exception);
        }
    }

    private void showActiveSessionsView() {
        if (activeSessionsView == null) {
            activeSessionsView = loadActiveSessionsView();
            contentArea.getChildren().add(activeSessionsView);
        } else {
            activeSessionsController.reloadSessions();
        }
        hideFeatureViews();
        informationPage.setVisible(false);
        informationPage.setManaged(false);
        activeSessionsView.setManaged(true);
        activeSessionsView.setVisible(true);
    }

    private Parent loadActiveSessionsView() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(ACTIVE_SESSIONS_VIEW));
        activeSessionsController = new ActiveSessionsController(getActiveSessionsUseCase, clock);
        loader.setController(activeSessionsController);
        try {
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load Active Sessions view", exception);
        }
    }

    private void hideFeatureViews() {
        setHidden(stationsView);
        setHidden(activeSessionsView);
    }

    private void setHidden(Parent view) {
        if (view != null) {
            view.setVisible(false);
            view.setManaged(false);
        }
    }
}
