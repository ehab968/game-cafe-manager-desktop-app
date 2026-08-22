package com.gamecafe.gamecafemanager.presentation.controller;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.domain.model.CompletedSessionsReport;
import com.gamecafe.gamecafemanager.domain.model.ProductSales;
import com.gamecafe.gamecafemanager.domain.model.ReportPeriod;
import com.gamecafe.gamecafemanager.domain.model.StationUsage;
import com.gamecafe.gamecafemanager.domain.usecase.report.GetReportUseCase;
import com.gamecafe.gamecafemanager.presentation.component.UiComponents;
import com.gamecafe.gamecafemanager.presentation.error.ApplicationErrorHandler;
import com.gamecafe.gamecafemanager.presentation.format.ApplicationDisplayService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class ReportsController {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM uuuu", Locale.getDefault());

    private final GetReportUseCase getReportUseCase;
    private final ApplicationDisplayService displayService;
    private final ApplicationErrorHandler errorHandler;
    private final ObservableList<StationUsage> stationUsage =
            FXCollections.observableArrayList();
    private final ObservableList<ProductSales> productSales =
            FXCollections.observableArrayList();

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Label periodLabel;
    @FXML private Label sessionsCountLabel;
    @FXML private Label gamingRevenueLabel;
    @FXML private Label productsRevenueLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label averageDurationLabel;
    @FXML private TableView<StationUsage> stationUsageTable;
    @FXML private TableColumn<StationUsage, String> stationNameColumn;
    @FXML private TableColumn<StationUsage, String> stationSessionsColumn;
    @FXML private TableColumn<StationUsage, String> stationDurationColumn;
    @FXML private TableView<ProductSales> productSalesTable;
    @FXML private TableColumn<ProductSales, String> productNameColumn;
    @FXML private TableColumn<ProductSales, String> productQuantityColumn;
    @FXML private TableColumn<ProductSales, String> productRevenueColumn;

    public ReportsController(
            GetReportUseCase getReportUseCase,
            ApplicationDisplayService displayService,
            ApplicationErrorHandler errorHandler) {
        this.getReportUseCase = Objects.requireNonNull(getReportUseCase, "getReportUseCase");
        this.displayService = Objects.requireNonNull(displayService, "displayService");
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
    }

    @FXML
    private void initialize() {
        stationUsageTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        productSalesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        stationNameColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getStationName()));
        stationSessionsColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                Long.toString(cell.getValue().getCompletedSessions())));
        stationDurationColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                formatDuration(cell.getValue().getTotalDuration())));
        stationUsageTable.setItems(stationUsage);

        productNameColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getProductName()));
        productQuantityColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                Long.toString(cell.getValue().getQuantitySold())));
        productRevenueColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                formatMoney(cell.getValue().getRevenue())));
        productSalesTable.setItems(productSales);
        stationUsageTable.setPlaceholder(UiComponents.emptyState(
                "No station activity",
                "Completed station sessions for the selected period will appear here."));
        productSalesTable.setPlaceholder(UiComponents.emptyState(
                "No product sales",
                "Products sold during completed sessions will appear here."));
        showToday();
    }

    @FXML
    private void showToday() {
        load(() -> getReportUseCase.executeToday());
    }

    @FXML
    private void showDaily() {
        load(() -> getReportUseCase.executeDay(startDatePicker.getValue()));
    }

    @FXML
    private void showRange() {
        load(() -> getReportUseCase.execute(
                startDatePicker.getValue(), endDatePicker.getValue()));
    }

    public void refresh() {
        showRange();
    }

    private void load(ReportSupplier supplier) {
        stationUsage.clear();
        productSales.clear();
        periodLabel.setText("Loading report…");
        periodLabel.getStyleClass().removeAll("feedback-success", "feedback-error");
        stationUsageTable.setPlaceholder(UiComponents.loadingState("Loading station activity…"));
        productSalesTable.setPlaceholder(UiComponents.loadingState("Loading product sales…"));
        try {
            display(supplier.get());
        } catch (ValidationException exception) {
            showLoadFailure(
                    "Choose a valid report period.",
                    "Report dates need attention.",
                    () -> load(supplier));
            showValidationErrors(exception);
        } catch (RuntimeException exception) {
            showLoadFailure(
                    "Report unavailable",
                    "Historical report data could not be loaded.",
                    () -> load(supplier));
            showError("Could not load report", exception);
        }
    }

    private void display(CompletedSessionsReport report) {
        ReportPeriod period = report.getPeriod();
        startDatePicker.setValue(period.getStartDate());
        endDatePicker.setValue(period.getEndDate());
        periodLabel.setText(formatPeriod(period));
        sessionsCountLabel.setText(Long.toString(report.getCompletedSessionsCount()));
        gamingRevenueLabel.setText(formatMoney(report.getGamingRevenue()));
        productsRevenueLabel.setText(formatMoney(report.getProductsRevenue()));
        totalRevenueLabel.setText(formatMoney(report.getTotalRevenue()));
        averageDurationLabel.setText(formatDuration(report.getAverageSessionDuration()));
        stationUsage.setAll(report.getStationUsage());
        productSales.setAll(report.getProductSales());
        stationUsageTable.setPlaceholder(UiComponents.emptyState(
                "No station activity",
                "No completed station sessions were found for this period."));
        productSalesTable.setPlaceholder(UiComponents.emptyState(
                "No product sales",
                "No products were sold during completed sessions in this period."));
    }

    private void showLoadFailure(String title, String message, Runnable retryAction) {
        periodLabel.setText(title);
        periodLabel.getStyleClass().removeAll("feedback-success", "feedback-error");
        periodLabel.getStyleClass().add("feedback-error");
        stationUsageTable.setPlaceholder(UiComponents.errorState(title, message, retryAction));
        productSalesTable.setPlaceholder(UiComponents.errorState(title, message, retryAction));
    }

    private String formatPeriod(ReportPeriod period) {
        if (period.getStartDate().equals(period.getEndDate())) {
            return DATE_FORMATTER.format(period.getStartDate());
        }
        return DATE_FORMATTER.format(period.getStartDate())
                + " – " + DATE_FORMATTER.format(period.getEndDate());
    }

    private String formatMoney(BigDecimal value) {
        return displayService.formatMoney(value);
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long hours = seconds / 3_600L;
        long minutes = (seconds % 3_600L) / 60L;
        long remainingSeconds = seconds % 60L;
        return String.format(
                Locale.ROOT, "%d:%02d:%02d", hours, minutes, remainingSeconds);
    }

    private void showValidationErrors(ValidationException exception) {
        showError("Check report dates", exception);
    }

    private void showError(String title, Throwable failure) {
        errorHandler.show(
                stationUsageTable.getScene() == null
                        ? null
                        : stationUsageTable.getScene().getWindow(),
                title,
                failure);
    }

    @FunctionalInterface
    private interface ReportSupplier {

        CompletedSessionsReport get();
    }
}
