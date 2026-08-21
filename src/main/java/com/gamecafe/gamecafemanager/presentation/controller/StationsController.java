package com.gamecafe.gamecafemanager.presentation.controller;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.domain.model.Station;
import com.gamecafe.gamecafemanager.domain.model.StationType;
import com.gamecafe.gamecafemanager.domain.usecase.session.GetActiveSessionsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.CreateStationUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.GetStationsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.SetStationEnabledUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.UpdateStationUseCase;
import java.math.BigDecimal;
import java.util.Map;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
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
            GetActiveSessionsUseCase getActiveSessionsUseCase) {
        this.createStationUseCase = Objects.requireNonNull(createStationUseCase, "createStationUseCase");
        this.updateStationUseCase = Objects.requireNonNull(updateStationUseCase, "updateStationUseCase");
        this.getStationsUseCase = Objects.requireNonNull(getStationsUseCase, "getStationsUseCase");
        this.setStationEnabledUseCase = Objects.requireNonNull(
                setStationEnabledUseCase, "setStationEnabledUseCase");
        this.getActiveSessionsUseCase = Objects.requireNonNull(
                getActiveSessionsUseCase, "getActiveSessionsUseCase");
    }

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getName()));
        typeColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getType().getDisplayName()));
        hourlyRateColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getHourlyRate().toPlainString()));
        statusColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(statusText(cell.getValue())));

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
            showError("Could not change station status", exception.getMessage());
        }
    }

    private void showStationDialog(Station existing) {
        boolean creating = existing == null;
        Dialog<Void> dialog = new Dialog<>();
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
        TextField hourlyRateField = new TextField(
                creating ? "" : existing.getHourlyRate().toPlainString());
        hourlyRateField.setPromptText("0.00");

        GridPane form = new GridPane();
        form.setHgap(12.0);
        form.setVgap(12.0);
        form.setPadding(new Insets(8.0, 0.0, 0.0, 0.0));
        form.addRow(0, new Label("Name"), nameField);
        form.addRow(1, new Label("Type"), typeField);
        form.addRow(2, new Label("Hourly price"), hourlyRateField);
        dialog.getDialogPane().setContent(form);

        ButtonType saveButtonType = new ButtonType(
                creating ? "Create" : "Save",
                ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        boolean[] saved = {false};

        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                BigDecimal hourlyRate = parseHourlyRate(hourlyRateField.getText());
                if (creating) {
                    createStationUseCase.execute(nameField.getText(), typeField.getValue(), hourlyRate);
                } else {
                    updateStationUseCase.execute(
                            existing.getId(),
                            nameField.getText(),
                            typeField.getValue(),
                            hourlyRate);
                }
                saved[0] = true;
            } catch (ValidationException exception) {
                showValidationErrors(exception);
                event.consume();
            } catch (RuntimeException exception) {
                showError("Could not save station", exception.getMessage());
                event.consume();
            }
        });

        dialog.showAndWait();
        if (saved[0]) {
            refreshStations();
        }
    }

    private BigDecimal parseHourlyRate(String value) {
        try {
            return new BigDecimal(value == null ? "" : value.trim());
        } catch (NumberFormatException exception) {
            throw ValidationException.forField("hourlyRate", "Enter a valid hourly price");
        }
    }

    public void refresh() {
        refreshStations();
    }

    private void refreshStations() {
        try {
            runningStationIds.clear();
            getActiveSessionsUseCase.execute().forEach(
                    session -> runningStationIds.add(session.getStationId()));
            stations.setAll(getStationsUseCase.execute());
            stationTable.refresh();
        } catch (RuntimeException exception) {
            showError("Could not load stations", exception.getMessage());
        }
    }

    private void updateActionState(Station station) {
        boolean noSelection = station == null;
        editButton.setDisable(noSelection);
        toggleEnabledButton.setDisable(noSelection || isRunning(station));
        toggleEnabledButton.setText(
                noSelection || station.isEnabled() ? "Disable" : "Enable");
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
        StringBuilder message = new StringBuilder();
        for (Map.Entry<String, String> error : exception.getErrors().entrySet()) {
            if (message.length() > 0) {
                message.append(System.lineSeparator());
            }
            message.append(error.getValue());
        }
        showError("Check station details", message.toString());
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Game Cafe Manager");
        alert.setHeaderText(title);
        alert.setContentText(message == null || message.trim().isEmpty()
                ? "The operation could not be completed."
                : message);
        if (stationTable.getScene() != null) {
            alert.initOwner(stationTable.getScene().getWindow());
        }
        alert.showAndWait();
    }
}
