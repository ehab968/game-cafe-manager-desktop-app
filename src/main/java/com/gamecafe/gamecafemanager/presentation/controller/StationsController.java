package com.gamecafe.gamecafemanager.presentation.controller;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.domain.model.Station;
import com.gamecafe.gamecafemanager.domain.model.StationType;
import com.gamecafe.gamecafemanager.domain.usecase.session.GetActiveSessionsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.CreateStationUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.GetStationsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.SetStationEnabledUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.UpdateStationUseCase;
import com.gamecafe.gamecafemanager.presentation.component.UiComponents;
import com.gamecafe.gamecafemanager.presentation.error.ApplicationErrorHandler;
import com.gamecafe.gamecafemanager.presentation.format.ApplicationDisplayService;
import com.gamecafe.gamecafemanager.presentation.style.UiStyles;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.beans.property.ReadOnlyStringWrapper;

/**
 * Presents Station CRUD operations without accessing JDBC or applying business
 * validation directly.
 */
public class StationsController {

    private final CreateStationUseCase createStationUseCase;
    private final UpdateStationUseCase updateStationUseCase;
    private final GetStationsUseCase getStationsUseCase;
    private final SetStationEnabledUseCase setStationEnabledUseCase;
    private final GetActiveSessionsUseCase getActiveSessionsUseCase;
    private final ApplicationDisplayService displayService;
    private final ApplicationErrorHandler errorHandler;
    private final ObservableList<Station> stations = FXCollections.observableArrayList();
    private final Set<Long> runningStationIds = new HashSet<>();

    @FXML
    private TableView<Station> stationTable;

    @FXML
    private TableColumn<Station, String> nameColumn;

    @FXML
    private TableColumn<Station, String> typeColumn;

    @FXML
    private TableColumn<Station, String> hourlyRateColumn;

    @FXML
    private TableColumn<Station, String> statusColumn;

    @FXML
    private Button editButton;

    @FXML
    private Button toggleEnabledButton;

    public StationsController(
            CreateStationUseCase createStationUseCase,
            UpdateStationUseCase updateStationUseCase,
            GetStationsUseCase getStationsUseCase,
            SetStationEnabledUseCase setStationEnabledUseCase,
            GetActiveSessionsUseCase getActiveSessionsUseCase,
            ApplicationDisplayService displayService,
            ApplicationErrorHandler errorHandler) {
        this.createStationUseCase = Objects.requireNonNull(createStationUseCase, "createStationUseCase");
        this.updateStationUseCase = Objects.requireNonNull(updateStationUseCase, "updateStationUseCase");
        this.getStationsUseCase = Objects.requireNonNull(getStationsUseCase, "getStationsUseCase");
        this.setStationEnabledUseCase = Objects.requireNonNull(
                setStationEnabledUseCase, "setStationEnabledUseCase");
        this.getActiveSessionsUseCase = Objects.requireNonNull(
                getActiveSessionsUseCase, "getActiveSessionsUseCase");
        this.displayService = Objects.requireNonNull(displayService, "displayService");
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
    }

    @FXML
    private void initialize() {
        stationTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        nameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getName()));
        typeColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getType().getDisplayName()));
        hourlyRateColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(formatStationPricing(cell.getValue())));
        statusColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(statusText(cell.getValue())));
        statusColumn.setCellFactory(UiComponents.statusCellFactory());

        stationTable.setItems(stations);
        stationTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> updateActionState(selected));
        updateActionState(null);
        refreshStations();
    }

    @FXML
    private void createStation() {
        showStationDialog(null);
    }

    @FXML
    private void editStation() {
        Station selected = stationTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showStationDialog(selected);
        }
    }

    @FXML
    private void toggleStationEnabled() {
        Station selected = stationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        try {
            setStationEnabledUseCase.execute(selected.getId(), !selected.isEnabled());
            refreshStations();
        } catch (RuntimeException exception) {
            showError("Could not change station status", exception);
        }
    }

    private void showStationDialog(Station existing) {
        boolean creating = existing == null;
        Dialog<Void> dialog = new Dialog<>();
        UiStyles.apply(dialog.getDialogPane());
        dialog.setTitle(creating ? "Create station" : "Edit station");
        dialog.setHeaderText(creating
                ? "Add a rentable station"
                : "Update " + existing.getName());
        dialog.initOwner(stationTable.getScene().getWindow());

        TextField nameField = new TextField(creating ? "" : existing.getName());
        ComboBox<StationType> typeField = new ComboBox<>(
                FXCollections.observableArrayList(StationType.values()));
        typeField.setMaxWidth(Double.MAX_VALUE);
        typeField.setValue(creating ? StationType.PLAYSTATION : existing.getType());
        TextField primaryRateField = new TextField(
                creating ? "" : existing.getHourlyRate().toPlainString());
        primaryRateField.setPromptText("0.00");
        TextField multiRateField = new TextField(
                creating || !existing.getType().supportsSessionModes()
                        ? ""
                        : existing.getMultiHourlyRate().toPlainString());
        multiRateField.setPromptText("0.00");
        Label primaryRateLabel = new Label();
        Label multiRateLabel = new Label("Multi hourly price");

        GridPane form = new GridPane();
        form.setHgap(12.0);
        form.setVgap(12.0);
        form.setPadding(new Insets(8.0, 0.0, 0.0, 0.0));
        form.addRow(0, new Label("Name"), nameField);
        form.addRow(1, new Label("Type"), typeField);
        form.addRow(2, primaryRateLabel, primaryRateField);
        form.addRow(3, multiRateLabel, multiRateField);
        updatePricingFields(
                typeField.getValue(), primaryRateLabel, multiRateLabel, multiRateField);
        typeField.valueProperty().addListener((observable, previous, selected) ->
                updatePricingFields(
                        selected, primaryRateLabel, multiRateLabel, multiRateField));
        dialog.getDialogPane().setContent(form);

        ButtonType saveButtonType = new ButtonType(
                creating ? "Create" : "Save",
                ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.getStyleClass().add("primary-button");
        boolean[] saved = {false};

        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                StationType selectedType = typeField.getValue();
                BigDecimal primaryRate = parseHourlyRate(
                        primaryRateField.getText(),
                        selectedType != null && selectedType.supportsSessionModes()
                                ? "singleHourlyRate"
                                : "hourlyRate",
                        selectedType != null && selectedType.supportsSessionModes()
                                ? "Single hourly price"
                                : "Hourly price");
                if (selectedType != null && selectedType.supportsSessionModes()) {
                    BigDecimal multiRate = parseHourlyRate(
                            multiRateField.getText(),
                            "multiHourlyRate",
                            "Multi hourly price");
                    if (creating) {
                        createStationUseCase.execute(
                                nameField.getText(), selectedType, primaryRate, multiRate);
                    } else {
                        updateStationUseCase.execute(
                                existing.getId(),
                                nameField.getText(),
                                selectedType,
                                primaryRate,
                                multiRate);
                    }
                } else if (creating) {
                    createStationUseCase.execute(
                            nameField.getText(), selectedType, primaryRate);
                } else {
                    updateStationUseCase.execute(
                            existing.getId(),
                            nameField.getText(),
                            selectedType,
                            primaryRate);
                }
                saved[0] = true;
            } catch (ValidationException exception) {
                showValidationErrors(exception);
                event.consume();
            } catch (RuntimeException exception) {
                showError("Could not save station", exception);
                event.consume();
            }
        });

        dialog.showAndWait();
        if (saved[0]) {
            refreshStations();
        }
    }

    private BigDecimal parseHourlyRate(String value, String field, String label) {
        try {
            return new BigDecimal(value == null ? "" : value.trim());
        } catch (NumberFormatException exception) {
            throw ValidationException.forField(field, "Enter a valid " + label.toLowerCase());
        }
    }

    private void updatePricingFields(
            StationType type,
            Label primaryRateLabel,
            Label multiRateLabel,
            TextField multiRateField) {
        boolean modesSupported = type != null && type.supportsSessionModes();
        primaryRateLabel.setText(modesSupported ? "Single hourly price" : "Hourly price");
        multiRateLabel.setVisible(modesSupported);
        multiRateLabel.setManaged(modesSupported);
        multiRateField.setVisible(modesSupported);
        multiRateField.setManaged(modesSupported);
    }

    private String formatStationPricing(Station station) {
        if (!station.getType().supportsSessionModes()) {
            return displayService.formatMoney(station.getHourlyRate());
        }
        return "Single " + displayService.formatMoney(station.getSingleHourlyRate())
                + " / Multi " + displayService.formatMoney(station.getMultiHourlyRate());
    }

    public void refresh() {
        refreshStations();
    }

    private void refreshStations() {
        stations.clear();
        stationTable.setPlaceholder(UiComponents.loadingState("Loading stations…"));
        try {
            runningStationIds.clear();
            getActiveSessionsUseCase.execute().forEach(
                    session -> runningStationIds.add(session.getStationId()));
            stations.setAll(getStationsUseCase.execute());
            stationTable.setPlaceholder(UiComponents.emptyState(
                    "No stations yet",
                    "Create a station to make it available on the dashboard."));
            stationTable.refresh();
        } catch (RuntimeException exception) {
            stationTable.setPlaceholder(UiComponents.errorState(
                    "Stations unavailable",
                    "Station data could not be loaded.",
                    this::refreshStations));
            showError("Could not load stations", exception);
        }
    }

    private void updateActionState(Station station) {
        boolean noSelection = station == null;
        editButton.setDisable(noSelection);
        toggleEnabledButton.setDisable(noSelection || isRunning(station));
        toggleEnabledButton.setText(
                noSelection || station.isEnabled() ? "Disable" : "Enable");
        toggleEnabledButton.getStyleClass().remove("danger-button");
        if (!noSelection && station.isEnabled()) {
            toggleEnabledButton.getStyleClass().add("danger-button");
        }
    }

    private boolean isRunning(Station station) {
        return station != null && runningStationIds.contains(station.getId());
    }

    private String statusText(Station station) {
        if (isRunning(station)) {
            return "Running";
        }
        return station.isEnabled() ? "Enabled" : "Disabled";
    }

    private void showValidationErrors(ValidationException exception) {
        showError("Check station details", exception);
    }

    private void showError(String title, Throwable failure) {
        errorHandler.show(
                stationTable.getScene() == null
                        ? null
                        : stationTable.getScene().getWindow(),
                title,
                failure);
    }
}
